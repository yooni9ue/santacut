package com.santacut.analysis

import com.santacut.api.docs.StreamAnalysisApi
import com.santacut.api.docs.SubmitAnalysisApi
import io.swagger.v3.oas.annotations.media.Schema
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import org.springframework.core.io.Resource
import org.springframework.ai.chat.client.ChatClient
import com.santacut.core.utcDatabaseTime
import com.santacut.core.utcIsoTimestamp
import com.santacut.core.AnalysisJobEntity
import com.santacut.core.AnalysisJobRepository
import com.santacut.core.AssessmentEntity
import com.santacut.core.AssessmentRepository
import com.santacut.core.DailyBudgetEntity
import com.santacut.core.DailyBudgetRepository
import com.santacut.core.ParticipantRepository
import com.santacut.core.ResultSnapshotRepository
import com.santacut.core.RankingService
import org.slf4j.LoggerFactory
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest
import java.security.Principal
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.springframework.scheduling.TaskScheduler

// Canonical v1 input. Platform export adapters must map only supported fields into this contract.
@Schema(description = "Origin of the normalized event data")
enum class DataSource { KAKAO, INSTAGRAM }

data class EventInput(
    @field:Schema(description = "Event time as an ISO-8601 UTC timestamp", type = "string", format = "date-time", example = "2026-12-01T08:30:00Z") val occurredAt: Instant,
    @field:Size(min=1,max=2000)
    @field:Schema(description = "Message text or normalized activity description; 1–2,000 characters", example = "친구가 힘들어해서 먼저 연락하고 이야기를 들어줬어.", minLength = 1, maxLength = 2000) val text: String
)

data class AnalysisInput(
    @field:Schema(description = "Export platform", example = "KAKAO") val source: DataSource,
    @field:Size(min=20,max=2000)
    @field:Schema(description = "20–2,000 normalized events, ordered by occurredAt when possible") val events: List<@Valid EventInput>
)

@Schema(description = "Analysis job state")
enum class AnalysisJobStatus { QUEUED, RUNNING, SUCCEEDED, FAILED, INSUFFICIENT_DATA }

data class AnalysisSubmissionResponse(
    @field:Schema(description = "Analysis job UUID", format = "uuid", example = "9ef3fca1-ec60-4c43-a231-d378d246ee34") val id: String,
    @field:Schema(description = "Initial state, or existing state for an idempotent replay", example = "QUEUED") val status: AnalysisJobStatus
)

data class AnalysisStatusResponse(
    @field:Schema(description = "Analysis job UUID", format = "uuid") val id: String,
    @field:Schema(description = "Current job state") val status: AnalysisJobStatus,
    @field:JsonProperty("error_code") @field:Schema(description = "Stable machine-readable error code; null until failed") val errorCode: String?,
    @field:JsonProperty("assessment_id") @field:Schema(description = "Assessment UUID after successful analysis") val assessmentId: String?,
    @field:JsonProperty("created_at") @field:Schema(description = "Job creation time in UTC", format = "date-time") val createdAt: String,
    @field:JsonProperty("finished_at") @field:Schema(description = "Terminal transition time in UTC; null while queued or running", format = "date-time") val finishedAt: String?
)

data class AnalysisResultResponse(
    @field:JsonProperty("score_hundredths") @field:Schema(description = "Weighted score in hundredths; divide by 100 for points", example = "8245", minimum = "0", maximum = "10000") val scoreHundredths: Int,
    @field:Schema(description = "Playful score verdict") val verdict: String,
    @field:Schema(description = "Playful explanation") val comment: String,
    @field:Schema(description = "Competition rank; equal scores share a rank") val rank: Int,
    @field:JsonProperty("delivery_sequence") @field:Schema(description = "Delivery order among participants") val deliverySequence: Int,
    @field:Schema(description = "Simulated delivery ETA in UTC", type = "string", format = "date-time", example = "2026-12-25T03:15:00.000Z") val eta: String
)

@RestController
@RequestMapping("/v1/analyses")
class AnalysisController(private val jobs: AnalysisJobs) {
    @PostMapping
    @SubmitAnalysisApi
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun create(principal: Principal, @RequestHeader("Idempotency-Key") key: String,
               @Valid @RequestBody input: AnalysisInput): AnalysisSubmissionResponse = jobs.submit(UUID.fromString(principal.name), key, input)
    @GetMapping("/{id}/events", produces=[org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE])
    @StreamAnalysisApi
    fun events(principal: Principal, @PathVariable id: UUID): SseEmitter = jobs.stream(UUID.fromString(principal.name), id)
}

@Component
@EnableScheduling
class AnalysisJobs(
    private val jobs: AnalysisJobRepository,
    private val participants: ParticipantRepository,
    private val assessments: AssessmentRepository,
    private val budgets: DailyBudgetRepository,
    private val snapshots: ResultSnapshotRepository,
    private val tx: TransactionTemplate,
    private val streamScheduler: TaskScheduler,
    private val mapper: ObjectMapper,
    private val ai: ScoreClient,
    private val ranking: RankingService,
    @Value("\${santacut.ai.daily-call-limit:100}") private val dailyLimit: Int,
    @Value("\${santacut.season-year:2026}") private val seasonYear: Int,
) {
    private val logger = LoggerFactory.getLogger(AnalysisJobs::class.java)

    fun submit(user: UUID, key: String, input: AnalysisInput): AnalysisSubmissionResponse {
        if (!key.matches(Regex("[A-Za-z0-9_-]{16,100}"))) fail(400,"INVALID_IDEMPOTENCY_KEY")
        val now = Instant.now()
        val cutoff = java.time.LocalDate.of(seasonYear,12,25).atStartOfDay(java.time.ZoneId.of("Asia/Seoul")).toInstant()
        if (!now.isBefore(cutoff)) fail(409,"SEASON_FROZEN")
        if (input.events.sumOf { it.text.length } > 200_000 || input.events.any {
                it.occurredAt.isAfter(now.plusSeconds(300)) || it.occurredAt.isBefore(now.minus(Duration.ofDays(90)))
            }) fail(400,"INVALID_EVENT_RANGE")
        val payload = mapper.writeValueAsString(input)
        val hash = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray()).joinToString("") { "%02x".format(it) }
        return tx.execute {
            if (!participants.existsById(user.toString())) fail(401,"NOT_AUTHENTICATED")
            val existing = jobs.findByUserIdAndIdempotencyKey(user.toString(), key)
            if (existing != null) {
                if(existing.requestHash != hash) fail(409,"IDEMPOTENCY_CONFLICT")
                return@execute AnalysisSubmissionResponse(existing.id, AnalysisJobStatus.valueOf(existing.status))
            }
            if (!ai.enabled()) fail(503,"AI_NOT_CONFIGURED")
            val recent = jobs.countByUserIdAndCreatedAtAfter(user.toString(),utcDatabaseTime(now.minus(Duration.ofDays(1))))
            if(recent >= 3) fail(429,"DAILY_USER_LIMIT")
            val id = UUID.randomUUID().toString()
            jobs.save(AnalysisJobEntity(id,user.toString(),key,hash,input.source.name,payload,createdAt=utcDatabaseTime(now)))
            AnalysisSubmissionResponse(id, AnalysisJobStatus.QUEUED)
        }
    }
    fun get(user: UUID,id: UUID): AnalysisStatusResponse {
        val job=jobs.findByIdAndUserId(id.toString(),user.toString()) ?: fail(404,"NOT_FOUND")
        return AnalysisStatusResponse(job.id,AnalysisJobStatus.valueOf(job.status),job.errorCode,job.assessmentId,
            utcIsoTimestamp(job.createdAt),job.finishedAt?.let(::utcIsoTimestamp))
    }
    fun stream(user: UUID,id: UUID): SseEmitter {
        get(user,id) // verify ownership before opening the stream
        val emitter=SseEmitter(180_000L)
        val polls=AtomicInteger()
        val finished=AtomicBoolean(false)
        val scheduled=AtomicReference<ScheduledFuture<*>?>()
        fun stop() {
            if (finished.compareAndSet(false, true)) emitter.complete()
            scheduled.get()?.cancel(false)
        }
        val task = Runnable {
            if (finished.get()) return@Runnable
            try {
                val state=get(user,id)
                emitter.send(SseEmitter.event().name("analysis").data(state))
                when(state.status) {
                    AnalysisJobStatus.SUCCEEDED -> {
                        val assessmentId=state.assessmentId ?: error("Successful job is missing its assessment")
                        val assessment=assessments.findById(assessmentId).orElse(null) ?: error("Successful job assessment is missing")
                        val snapshot=snapshots.findFirstByAssessmentIdOrderByComputedAtDesc(assessmentId) ?: error("Successful job ranking is missing")
                        val result=AnalysisResultResponse(assessment.scoreHundredths,assessment.verdict,assessment.comment,snapshot.rank,snapshot.deliverySequence,utcIsoTimestamp(snapshot.eta))
                        emitter.send(SseEmitter.event().name("result").data(result)); stop()
                    }
                    AnalysisJobStatus.FAILED, AnalysisJobStatus.INSUFFICIENT_DATA -> { emitter.send(SseEmitter.event().name("error").data(state)); stop() }
                    AnalysisJobStatus.QUEUED, AnalysisJobStatus.RUNNING -> if(polls.incrementAndGet() >= 150) stop()
                }
            } catch (_: Exception) {
                finished.set(true)
                emitter.completeWithError(RuntimeException("Analysis event stream closed"))
                scheduled.get()?.cancel(false)
            }
        }
        scheduled.set(streamScheduler.scheduleAtFixedRate(task, Instant.now().plusSeconds(1), Duration.ofSeconds(1)))
        emitter.onCompletion { finished.set(true); scheduled.get()?.cancel(false) }
        emitter.onTimeout { finished.set(true); scheduled.get()?.cancel(false) }
        emitter.onError { finished.set(true); scheduled.get()?.cancel(false) }
        return emitter
    }
    @Scheduled(fixedDelayString="\${santacut.ai.poll-delay-ms:3000}")
    fun poll() {
        if(!ai.enabled()) return
        val lease=UUID.randomUUID().toString()
        val job=tx.execute { claimJob(lease) } ?: return
        try {
            val score=ai.score(job.payload ?: error("Queued analysis payload is missing"))
            tx.executeWithoutResult {
                val held=jobs.findById(job.id).orElse(null) ?: return@executeWithoutResult
                if(held.leaseToken != lease || held.status != "RUNNING") return@executeWithoutResult
                val cutoff=java.time.LocalDate.of(seasonYear,12,25).atStartOfDay(java.time.ZoneId.of("Asia/Seoul")).toInstant()
                val completedAt=Instant.now()
                if(!completedAt.isBefore(cutoff)) {
                    held.status="FAILED"; held.errorCode="SEASON_FROZEN"; held.payload=null; held.finishedAt=utcDatabaseTime(completedAt); jobs.save(held)
                    return@executeWithoutResult
                }
                val assessmentId=UUID.randomUUID().toString()
                assessments.save(AssessmentEntity(assessmentId,held.userId,held.source,score.hundredths,score.verdict,score.comment,mapper.writeValueAsString(score.components),utcDatabaseTime(completedAt)))
                held.status="SUCCEEDED"; held.assessmentId=assessmentId; held.payload=null; held.finishedAt=utcDatabaseTime(completedAt); jobs.save(held)
                // Publish the ranking in the same transaction as the successful assessment.
                // If rebuilding fails, both writes roll back and the worker can mark the job failed.
                ranking.rebuild(completedAt)
            }
        } catch (exception: Exception) {
            // Never log provider bodies or submitted messages. Failed jobs require deliberate retry via a new request.
            val insufficient = exception is InsufficientEvidenceException
            val reason = when {
                insufficient -> "insufficient evidence"
                exception is IllegalArgumentException && exception.message?.startsWith("AI_RESPONSE_SCHEMA_INVALID") == true -> exception.message
                exception is IllegalArgumentException && exception.message?.startsWith("AI_EVIDENCE_INVALID") == true -> exception.message
                exception is IllegalArgumentException && exception.message in setOf("AI_RESPONSE_INVALID", "AI_SCORE_OUT_OF_RANGE") -> exception.message
                else -> exception.javaClass.simpleName
            }
            logger.warn("Analysis job {} ended: {}", job.id, reason)
            tx.executeWithoutResult {
                jobs.findById(job.id).orElse(null)?.takeIf { it.leaseToken == lease && it.status == "RUNNING" }?.let {
                    it.status=if (insufficient) "INSUFFICIENT_DATA" else "FAILED"
                    it.errorCode=if (insufficient) "INSUFFICIENT_EVIDENCE" else "ANALYSIS_FAILED"
                    it.payload=null; it.finishedAt=utcDatabaseTime(Instant.now()); jobs.save(it)
                }
            }
        }
    }
    private fun claimJob(lease: String): AnalysisJobEntity? {
        val now=Instant.now(); val current=utcDatabaseTime(now)
        jobs.findFirstByStatusAndCreatedAtBeforeOrderByCreatedAtAsc("QUEUED",utcDatabaseTime(now.minus(Duration.ofHours(24))))?.let {
            it.status="FAILED"; it.errorCode="EXPIRED"; it.payload=null; it.finishedAt=current; jobs.save(it)
        }
        jobs.findFirstByStatusAndLeaseUntilBeforeAndAttemptsGreaterThanEqualOrderByCreatedAtAsc("RUNNING",current,3)?.let {
            it.status="FAILED"; it.errorCode="RETRY_EXHAUSTED"; it.payload=null; it.finishedAt=current; jobs.save(it)
        }
        val job=jobs.findFirstByStatusOrderByCreatedAtAsc("QUEUED")
            ?: jobs.findFirstByStatusAndLeaseUntilBeforeAndAttemptsLessThanOrderByCreatedAtAsc("RUNNING",current,3)
            ?: return null
        val day=java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul")).toString()
        val budget=budgets.findById(day).orElse(DailyBudgetEntity(day,0))
        if(budget.calls>=dailyLimit) return null
        budget.calls++
        budgets.save(budget)
        job.status="RUNNING"; job.attempts++; job.leaseToken=lease; job.leaseUntil=utcDatabaseTime(now.plus(Duration.ofMinutes(3)))
        return jobs.save(job)
    }
    private fun fail(status:Int,code:String): Nothing = throw ResponseStatusException(HttpStatus.valueOf(status),code)
}

data class ScoredResult(val hundredths: Int,val components: List<BigDecimal>) {
    val verdict: String get() = when { hundredths>=9000 -> "산타 친자 의혹"; hundredths>=7500 -> "선물 배송 출발"; hundredths>=5000 -> "굴뚝 앞 배송 보류"; else -> "석탄 착불 배송" }
    val comment: String get() = when { hundredths>=9000 -> "이 정도면 선물 말고 상속을 기다려라."; hundredths>=7500 -> "착한 척인지는 모르겠는데 일단 속아드림."; hundredths>=5000 -> "산타가 네 선물 찾느라 안경을 다시 씀."; else -> "크리스마스 선물인데 새해 복 많이 받아라." }
}

private class InsufficientEvidenceException : RuntimeException()

@Component
class ScoreClient(
    private val mapper: ObjectMapper,
    builder: ChatClient.Builder,
    @Value("classpath:prompts/score-v1.txt") private val promptResource: Resource,
    @Value("\${spring.ai.ollama.chat.options.model:qwen2.5:3b}") private val model: String
) {
    private val chat = builder.build()
    private val prompt by lazy { promptResource.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() } }
    fun enabled() = model.isNotBlank()
    fun score(payload: String): ScoredResult {
        check(enabled())
        val submitted=mapper.readTree(payload)
        val inputEvents=submitted.path("events")
        val indexedEvents=(0 until inputEvents.size()).map { index ->
            val event=inputEvents.get(index)
            mapOf("index" to index+1,"occurredAt" to event.get("occurredAt"),"text" to event.get("text"))
        }
        val modelPayload=mapper.writeValueAsString(mapOf("source" to submitted.path("source"),"events" to indexedEvents))
        val content = chat.prompt().system(prompt).user(modelPayload).call().content()
        require(!content.isNullOrBlank() && content.length <= 100_000) { "AI_RESPONSE_INVALID" }
        val result=mapper.readTree(content)
        if (!result.path("sufficient").asBoolean(false)) throw InsufficientEvidenceException()
        val dimensionNames = listOf("care_empathy", "positivity_support", "respectful_communication", "cooperation_sharing")
        val scoreNode=result.path("scores")
        val evidenceNode=result.path("evidence")
        val values=when {
            scoreNode.isObject -> dimensionNames.map { scoreNode.path(it) }
            scoreNode.isArray && scoreNode.size()==dimensionNames.size -> (0 until scoreNode.size()).map(scoreNode::get)
            else -> emptyList()
        }
        val evidence=when {
            evidenceNode.isObject -> dimensionNames.map { evidenceNode.path(it) }
            evidenceNode.isArray && evidenceNode.size()==dimensionNames.size -> (0 until evidenceNode.size()).map(evidenceNode::get)
            else -> emptyList()
        }
        val count=inputEvents.size()
        val evidenceIndices=evidence.map { if(it.isArray) (0 until it.size()).map(it::get) else listOf(it) }
        require(values.size==dimensionNames.size && values.all { it.isNumber } && evidenceIndices.size==dimensionNames.size && evidenceIndices.all { it.isNotEmpty() }) {
            "AI_RESPONSE_SCHEMA_INVALID:scores=${scoreNode.nodeType},evidence=${evidenceNode.nodeType}"
        }
        val evidenceIndicesOneBased=evidenceIndices.map { dimension -> dimension.map { node ->
            when {
                node.isIntegralNumber -> node.asInt()
                node.isNumber && node.decimalValue().stripTrailingZeros().scale() <= 0 -> runCatching { node.decimalValue().intValueExact() }.getOrDefault(-1)
                node.nodeType.name == "STRING" -> node.toString().trim('"').toIntOrNull() ?: -1
                else -> -1
            }
        } }
        val invalidEvidence=evidenceIndicesOneBased.flatten().firstOrNull { it !in 1..count }
        require(invalidEvidence==null) { "AI_EVIDENCE_INVALID:count=$count,index=$invalidEvidence" }
        val scores=values.map { node -> node.decimalValue().also { n -> require(n>=BigDecimal.ZERO && n<=BigDecimal(100) && n.scale()<=2) { "AI_SCORE_OUT_OF_RANGE" } } }
        val weights=listOf(30,25,25,20)
        val hundredths=scores.zip(weights).fold(BigDecimal.ZERO) { sum,(score,weight) -> sum+score*BigDecimal(weight) }.setScale(0,RoundingMode.HALF_UP).intValueExact()
        return ScoredResult(hundredths,scores)
    }
}


