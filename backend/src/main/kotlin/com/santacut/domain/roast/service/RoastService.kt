package com.santacut.domain.roast.service

import com.santacut.domain.analysis.model.AnalysisJob
import com.santacut.domain.analysis.model.AnalysisSubmission
import com.santacut.domain.analysis.model.Assessment
import com.santacut.domain.analysis.model.GiftVerdict
import com.santacut.domain.analysis.model.ModelUsage
import com.santacut.domain.analysis.model.ProviderBackoff
import com.santacut.domain.analysis.model.ScoringFailure
import com.santacut.domain.dossier.model.DossierCatalog
import com.santacut.domain.dossier.model.DossierDraft
import com.santacut.domain.dossier.model.DossierStats
import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import com.santacut.domain.global.model.SeoulTime
import com.santacut.domain.prompt.model.Prompt
import com.santacut.domain.roast.model.Roast
import com.santacut.domain.roast.model.RoastBrief
import com.santacut.domain.roast.model.RoastCatalog
import com.santacut.domain.roast.model.RoastContent
import com.santacut.domain.roast.model.RoastDraft
import com.santacut.domain.roast.model.RoastReview
import com.santacut.domain.roast.model.RoastRewrite
import com.santacut.domain.roast.model.RoastSheet
import com.santacut.infrastructure.global.config.RoastModelProperties
import com.santacut.infrastructure.roast.client.RoastClient
import com.santacut.infrastructure.roast.repository.RoastRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * 「팩폭 산타」 작가 문장 쓰기와 저장. 입력 조립·검증·대체 규칙은 도메인 모델([RoastBrief], [RoastReview], [RoastSheet])이 정한다.
 *
 * 작가를 부르면 응답을 필드마다 검증하고, 떨어진 필드가 있으면 사유를 붙여 최대 설정한 횟수만큼 다시 불러 그 필드만 갈아 끼운다.
 * 그래도 빈 필드는 서버 대체 문장으로 채운다. 호출 자체가 실패하면(제공자 오류, 한도 초과, 시간 초과) 다시 쓰지 않고 바로 대체 문장으로 넘어가며,
 * 연달아 [MAX_CONSECUTIVE_FAILURES]번 실패하면 [PAUSE] 동안 작가를 부르지 않는다. 로그에는 호출 수, 토큰 수, 필드 이름과 수만 남기고 문장·원문·사유는 남기지 않는다.
 */
@Service
class RoastService(
    private val roastRepository: RoastRepository,
    private val roastClient: RoastClient,
    private val catalog: RoastCatalog,
    private val dossierCatalog: DossierCatalog,
    private val objectMapper: ObjectMapper,
    private val properties: RoastModelProperties,
    private val clock: Clock,
) {
    private val logger = LoggerFactory.getLogger(RoastService::class.java)

    /** 동시에 부른 호출의 사용량을 하나씩 기록하게 하는 잠금. */
    private val usageLock = Any()

    /** 작가 제공자 쪽 연속 실패로 작가 호출을 잠시 멈추는 상태. 채점 쪽 멈춤과 따로 센다. */
    private val backoff = ProviderBackoff(PAUSE, MAX_CONSECUTIVE_FAILURES)

    /** 작가 모델이 설정돼 있고 연속 실패로 멈춘 동안이 아닌지. 아니면 작가를 부르지 않고 대체 문장만 쓴다. */
    fun isEnabled(): Boolean = roastClient.isEnabled() && !backoff.isPausedAt(clock.instant())

    /** 작업 페이로드의 메시지와 미리 만든 조서 [dossier], 총점 [hundredths]로 작가 입력을 조립한다. 작업 성공이 페이로드를 지우므로 그 전에 부른다. */
    fun brief(job: AnalysisJob, hundredths: Int, dossier: DossierDraft?): RoastBrief {
        val submission = objectMapper.readValue(job.requirePayload(), AnalysisSubmission::class.java)
        val messages = submission.events.toScoringSequence(SeoulTime.ZONE).toList()
        return RoastBrief.compose(messages, statsOf(dossier), GiftVerdict(hundredths), catalog, dossierCatalog)
    }

    /** 호출 전 비용 확인에 쓰는 어림 사용량. 다시 쓰기까지 최대 호출 수만큼이다. */
    fun estimateUsage(brief: RoastBrief, prompt: Prompt): ModelUsage {
        val once = roastClient.estimateUsage(prompt.content, brief.inputText(objectMapper))
        val calls = properties.maxCalls().toLong()
        return ModelUsage(once.inputTokens * calls, once.outputTokens * calls, once.finishReason)
    }

    /** [prompt] 판으로 작가를 부르고, 떨어진 필드는 다시 쓰게 해 통과한 문장을 모은다. 호출마다 사용량을 [recordUsage]에 넘긴다. */
    fun ask(brief: RoastBrief, prompt: Prompt, recordUsage: (ModelUsage) -> Unit): RoastSheet =
        refine(brief, prompt, recordUsage, RoastSheet.startFor(brief), null, 1)

    /** 모은 문장 [sheet]의 빈칸을 대체 문장으로 채워 저장할 결과를 만든다. 작가를 부르지 않았으면 [prompt]가 null이다. */
    fun draft(brief: RoastBrief, sheet: RoastSheet, prompt: Prompt?, seed: Int): RoastDraft {
        val content = sheet.toContent(brief, catalog.fallback, seed)
        logger.info(
            "팩폭 결과: 프롬프트 {}판, 사실 {}개, 줄 {}개, 빈 필드 {}, 대체 문장 {}개",
            prompt?.version, brief.facts.size(), brief.lineCount(), sheet.missingFields(), content.fallbackCount(),
        )
        return RoastDraft(objectMapper.writeValueAsString(content), prompt?.version, if (prompt == null) "" else roastClient.model())
    }

    /** 작가 없이 대체 문장만으로 만든 결과. 조서 집계가 있으면 그 사실로 짝과 칩을 채운다. 작가가 실패하거나 꺼졌을 때, 기능 전 평가를 재사용할 때 쓴다. */
    fun fallbackDraft(hundredths: Int, dossier: DossierDraft?, seed: Int): RoastDraft =
        draft(RoastBrief.compose(emptyList(), statsOf(dossier), GiftVerdict(hundredths), catalog, dossierCatalog), RoastSheet.EMPTY, null, seed)

    /** [assessment]의 팩폭 결과를 저장한다. */
    fun record(assessment: Assessment, draft: RoastDraft, recordedAt: Instant) {
        roastRepository.save(Roast.record(assessment, draft, recordedAt))
    }

    /**
     * 같은 채점 입력으로 다시 낸 작업이 지난 평가([previousAssessmentId])를 옮겨 적었을 때 지난 팩폭 결과를 새 평가에 복사한다.
     * 지난 결과가 없으면(이 기능 전 평가) 작가 없이 대체 문장으로 저장한다.
     */
    fun reuse(previousAssessmentId: String, assessment: Assessment, dossier: DossierDraft?, recordedAt: Instant) {
        val previous = roastRepository.findByAssessmentId(previousAssessmentId)
        if (previous != null) {
            roastRepository.save(previous.copyFor(assessment, recordedAt))
            return
        }
        record(assessment, fallbackDraft(assessment.scoreHundredths, dossier, seedOf(assessment.id)), recordedAt)
    }

    /** [assessmentId] 평가의 팩폭 결과. 이 기능 전 평가라 없으면 [ErrorCode.RECAP_NOT_FOUND]다. */
    fun get(assessmentId: String): RoastContent {
        val roast = roastRepository.findByAssessmentId(assessmentId) ?: throw SantaCutException(ErrorCode.RECAP_NOT_FOUND)
        return objectMapper.readValue(roast.contentJson, RoastContent::class.java)
    }

    /** 대체 문장을 고르는 시드. 같은 작업·평가에는 늘 같은 문장이다. */
    fun seedOf(id: String): Int = id.hashCode()

    private tailrec fun refine(
        brief: RoastBrief,
        prompt: Prompt,
        recordUsage: (ModelUsage) -> Unit,
        sheet: RoastSheet,
        rewrite: RoastRewrite?,
        call: Int,
    ): RoastSheet {
        val answer = roastClient.request(prompt.content, brief.inputText(objectMapper, rewrite))
        recordSafely(answer.usage, recordUsage)
        if (answer.failed) {
            backoff.record(ScoringFailure.ANALYSIS_ERROR, clock.instant())
            logger.warn("팩폭 작가 {}번째 호출이 실패해 다시 쓰지 않고 대체 문장으로 넘어간다", call)
            return sheet
        }
        backoff.recordSuccess()
        val review = RoastReview.of(answer.text, brief, catalog.rules, objectMapper)
        val next = sheet.absorb(review)
        logCall(call, answer.usage, review, next)
        if (next.isComplete() || call >= properties.maxCalls()) return next
        return refine(brief, prompt, recordUsage, next, next.rewriteFor(review), call + 1)
    }

    /** 숫자와 필드 이름만 남긴다. */
    private fun logCall(call: Int, usage: ModelUsage, review: RoastReview, sheet: RoastSheet) {
        logger.info(
            "팩폭 작가 {}번째 호출: 입력 {}(캐시 {}) 출력 {} 생각 {} 토큰, 종료 {}, 떨어진 필드 {}, 남은 빈 필드 {}",
            call, usage.inputTokens, usage.cachedInputTokens, usage.outputTokens, usage.thoughtTokens, usage.finishReason,
            review.checks.failedFields(), sheet.missingFields(),
        )
    }

    /** 사용량 기록 실패(일시적인 DB 오류 등)가 받은 답을 버리게 하지 않는다. */
    private fun recordSafely(usage: ModelUsage, recordUsage: (ModelUsage) -> Unit) {
        try {
            synchronized(usageLock) { recordUsage(usage) }
        } catch (exception: RuntimeException) {
            logger.warn("팩폭 작가 사용량 기록 실패, 받은 답은 그대로 쓴다: {}", exception.javaClass.simpleName)
        }
    }

    /** 조서 초안의 집계값. 초안이 없거나 읽을 수 없으면 null이고, 사실·인용·화제 없이 쓴다. */
    private fun statsOf(dossier: DossierDraft?): DossierStats? = dossier?.let { draft ->
        try {
            objectMapper.readValue(draft.statsJson, DossierStats::class.java)
        } catch (exception: RuntimeException) {
            logger.warn("팩폭 입력용 조서 집계를 읽지 못해 사실 없이 쓴다: {}", exception.javaClass.simpleName)
            null
        }
    }

    companion object {
        /** 작가 호출이 이만큼 연달아 실패하면 잠시 멈춘다. */
        const val MAX_CONSECUTIVE_FAILURES = 3

        /** 연속 실패 뒤 작가를 부르지 않는 시간. */
        val PAUSE: Duration = Duration.ofMinutes(5)
    }
}
