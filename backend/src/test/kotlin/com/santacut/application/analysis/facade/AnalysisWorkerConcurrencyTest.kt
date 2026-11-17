package com.santacut.application.analysis.facade

import com.santacut.MutableClock
import com.santacut.TestParticipants.REGION_CODE
import com.santacut.TestParticipants.uniqueNickname
import com.santacut.application.analysis.command.AnalysisEventCommand
import com.santacut.application.analysis.command.AnalysisEventCommands
import com.santacut.application.analysis.command.AnalysisJobCommand
import com.santacut.application.analysis.command.SubmitAnalysisCommand
import com.santacut.application.participant.command.RegisterParticipantCommand
import com.santacut.application.participant.facade.ParticipantFacade
import com.santacut.domain.analysis.model.AnalysisEvents
import com.santacut.domain.analysis.model.AnalysisSubmission
import com.santacut.domain.analysis.model.DataSource
import com.santacut.domain.analysis.model.ModelUsage
import com.santacut.domain.analysis.model.ScoreAnswer
import com.santacut.domain.analysis.model.ScoringChunk
import com.santacut.domain.analysis.model.ScoringDigest
import com.santacut.domain.analysis.model.TranscriptTurns
import com.santacut.domain.analysis.service.AiBudgetService
import com.santacut.domain.prompt.model.Prompt
import com.santacut.domain.prompt.model.PromptName
import com.santacut.infrastructure.analysis.client.ScoreClient
import com.santacut.infrastructure.analysis.repository.AiUsageDayRepository
import com.santacut.infrastructure.analysis.repository.AnalysisJobRepository
import com.santacut.infrastructure.prompt.repository.PromptRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.willReturn
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * 분석 작업자를 여러 스레드로 돌릴 때 같은 작업을 두 번 잡지 않는지, 동시에 비용 확인을 통과해 한도를 넘지 않는지,
 * 예약 폴링이 설정한 수만큼만 동시에 채점하는지 확인한다. AI 호출은 잠깐 멈추는 목으로 바꿔 작업들이 실제로 겹치게 한다.
 * 대기열 전체를 보는 작업자라 다른 통합 테스트와 섞이지 않게 `build/worker-concurrency-test.db`를 따로 쓰고 테스트마다 비운다.
 */
@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:sqlite:./build/worker-concurrency-test.db",
        "app.season-year=2099",
        "santacut.ai.poll-delay-ms=3600000",
        "santacut.upload.poll-delay-ms=3600000",
        "santacut.scheduling.enabled=false",
        "santacut.score.sampling.samples=1",
        "santacut.ai.worker.concurrency=${AnalysisWorkerConcurrencyTest.WORKER_CONCURRENCY}",
    ],
)
@Import(WorkerTestClockConfiguration::class)
@MockitoBean(types = [ScoreClient::class])
class AnalysisWorkerConcurrencyTest @Autowired constructor(
    private val analysisWorkerFacade: AnalysisWorkerFacade,
    private val analysisFacade: AnalysisFacade,
    private val participantFacade: ParticipantFacade,
    private val scoreClient: ScoreClient,
    private val clock: MutableClock,
    private val analysisJobRepository: AnalysisJobRepository,
    private val promptRepository: PromptRepository,
    private val aiUsageDayRepository: AiUsageDayRepository,
    private val aiBudgetService: AiBudgetService,
) {
    @BeforeEach
    fun setUp() {
        analysisJobRepository.deleteAll()
        aiUsageDayRepository.deleteAll()
        promptRepository.deleteAll()
        promptRepository.save(Prompt.initial(PromptName.SCORE, PROMPT, OPEN_TIME))
        clock.moveTo(OPEN_TIME)
        aiBudgetService.recordSuccess()
        given(scoreClient.isEnabled()).willReturn(true)
        given(scoreClient.cacheKey(anyDigest(), anyInt())).willReturn(CACHE_KEY)
        given(scoreClient.estimateUsage(anyChunk(), anyString())).willReturn(ESTIMATE)
    }

    @Test
    fun `여러 스레드가 동시에 작업을 잡아도 한 작업은 한 번만 잡혀 한 번만 채점된다`() {
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willAnswer { slowAnswer(USAGE) }
        val jobs = List(JOB_COUNT) { submitAnalysis() }

        runConcurrently(THREAD_COUNT) { analysisWorkerFacade.processNextJob() }

        assertEquals(List(JOB_COUNT) { "SUCCEEDED" }, jobs.map { analysisFacade.getStatus(it).status })
        assertEquals(List(JOB_COUNT) { 1 }, jobs.map { analysisJobRepository.findById(it.jobId.toString()).orElseThrow().attempts })
        verify(scoreClient, times(JOB_COUNT)).requestAnswer(anyChunk(), anyString())
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun `동시에 채점해도 다른 작업이 예약한 어림 비용까지 더해 한 달 한도를 넘는 호출은 하지 않는다`() {
        given(scoreClient.estimateUsage(anyChunk(), anyString())).willReturn(QUARTER_BUDGET_PLUS)
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willAnswer { slowAnswer(QUARTER_BUDGET_PLUS) }
        val jobs = List(JOB_COUNT) { submitAnalysis() }

        runConcurrently(JOB_COUNT) { analysisWorkerFacade.processNextJob() }

        verify(scoreClient, times(AFFORDABLE_JOB_COUNT)).requestAnswer(anyChunk(), anyString())
        assertEquals(AFFORDABLE_JOB_COUNT, jobs.count { analysisFacade.getStatus(it).status == "SUCCEEDED" })
        assertEquals(QUARTER_BUDGET_PLUS.inputTokens * AFFORDABLE_JOB_COUNT, aiUsageDayRepository.findAll().sumOf { it.inputTokens })
    }

    @Test
    fun `예약 폴링은 설정한 수만큼만 작업을 동시에 채점하고 대기열이 빌 때까지 이어서 채점한다`() {
        val running = AtomicInteger()
        val peak = AtomicInteger()
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willAnswer {
            peak.accumulateAndGet(running.incrementAndGet(), ::maxOf)
            slowAnswer(USAGE).also { running.decrementAndGet() }
        }
        val jobs = List(JOB_COUNT) { submitAnalysis() }

        analysisWorkerFacade.dispatchJobs()

        awaitUntil { jobs.all { analysisFacade.getStatus(it).status == "SUCCEEDED" } }
        assertEquals(WORKER_CONCURRENCY, peak.get())
    }

    @Test
    fun `작업을 잡다가 예외가 나도 채점 스레드 자리를 잃지 않는다`() {
        given(scoreClient.isEnabled()).willThrow(IllegalStateException("AI 설정 읽기 실패"))
        repeat(WORKER_CONCURRENCY) { assertFailsWith<IllegalStateException> { analysisWorkerFacade.dispatchJobs() } }
        // 예외를 던지게 스텁한 메서드를 다시 부르지 않도록 willReturn부터 쓴다.
        willReturn(true).given(scoreClient).isEnabled()
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willAnswer { slowAnswer(USAGE) }
        val jobs = List(JOB_COUNT) { submitAnalysis() }

        analysisWorkerFacade.dispatchJobs()

        awaitUntil { jobs.all { analysisFacade.getStatus(it).status == "SUCCEEDED" } }
    }

    @Test
    fun `채점 중 스택이 넘친 작업은 ANALYSIS_FAILED로 끝나고 채점 스레드 자리는 모두 돌아온다`() {
        val calls = AtomicInteger()
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willAnswer {
            if (calls.getAndIncrement() < WORKER_CONCURRENCY) throw StackOverflowError()
            slowAnswer(USAGE)
        }
        val jobs = List(JOB_COUNT + WORKER_CONCURRENCY) { submitAnalysis() }

        analysisWorkerFacade.dispatchJobs()

        awaitUntil { jobs.none { analysisFacade.getStatus(it).status in UNFINISHED } }
        assertEquals(WORKER_CONCURRENCY, jobs.count { analysisFacade.getStatus(it).errorCode == "ANALYSIS_FAILED" })
        assertEquals(JOB_COUNT, jobs.count { analysisFacade.getStatus(it).status == "SUCCEEDED" })
        assertEquals(List(jobs.size) { 1 }, jobs.map { analysisJobRepository.findById(it.jobId.toString()).orElseThrow().attempts })
        assertEquals(WORKER_CONCURRENCY, peakConcurrency())
    }

    /** 대기열을 새로 채워 예약 폴링을 한 번 돌리고, 동시에 채점한 작업 수의 최댓값을 돌려준다. 자리가 새면 설정한 수보다 작다. */
    private fun peakConcurrency(): Int {
        val running = AtomicInteger()
        val peak = AtomicInteger()
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willAnswer {
            peak.accumulateAndGet(running.incrementAndGet(), ::maxOf)
            slowAnswer(USAGE).also { running.decrementAndGet() }
        }
        val jobs = List(JOB_COUNT) { submitAnalysis() }
        analysisWorkerFacade.dispatchJobs()
        awaitUntil { jobs.all { analysisFacade.getStatus(it).status == "SUCCEEDED" } }
        return peak.get()
    }

    private fun slowAnswer(usage: ModelUsage): ScoreAnswer {
        Thread.sleep(CALL_MILLIS)
        return ScoreAnswer.of(List(DIMENSION_COUNT) { DIMENSION_SCORE }, List(DIMENSION_COUNT) { listOf(1) }, "test", usage)
    }

    /** [threadCount]개 스레드가 한꺼번에 [action]을 시작하게 하고 모두 끝날 때까지 기다린다. */
    private fun runConcurrently(threadCount: Int, action: () -> Unit) {
        val start = CountDownLatch(1)
        Executors.newFixedThreadPool(threadCount).use { executor ->
            val futures = List(threadCount) { executor.submit { start.await(); action() } }
            start.countDown()
            futures.forEach { it.get(WAIT_SECONDS, TimeUnit.SECONDS) }
        }
    }

    private fun awaitUntil(condition: () -> Boolean) {
        val deadline = System.nanoTime() + Duration.ofSeconds(WAIT_SECONDS).toNanos()
        while (!condition() && System.nanoTime() < deadline) Thread.sleep(POLL_MILLIS)
        assertTrue(condition())
    }

    private fun submitAnalysis(): AnalysisJobCommand {
        val participantId = participantFacade.register(RegisterParticipantCommand(uniqueNickname(), REGION_CODE)).id
        val occurredAt = clock.instant().minus(Duration.ofDays(1))
        val events = AnalysisEventCommands(
            List(EVENT_COUNT) { index -> AnalysisEventCommand(occurredAt.minus(Duration.ofHours(EVENT_SPACING_HOURS * index)), "$SAMPLE_TEXT $index") },
        )
        val command = SubmitAnalysisCommand(participantId, "key-" + UUID.randomUUID(), DataSource.KAKAO.name, events)
        return AnalysisJobCommand(participantId, UUID.fromString(analysisFacade.submit(command).id))
    }

    private fun anyDigest(): ScoringDigest =
        any(ScoringDigest::class.java) ?: AnalysisSubmission(DataSource.KAKAO, AnalysisEvents(emptyList())).toScoringDigest()

    private fun anyChunk(): ScoringChunk = any(ScoringChunk::class.java) ?: ScoringChunk(TranscriptTurns(emptyList()), 1, 0, 0)

    companion object {
        const val WORKER_CONCURRENCY = 3
        private const val JOB_COUNT = 4
        private const val THREAD_COUNT = 7

        /** 한 달 한도 9달러에서 어림 비용 3.6달러인 작업은 두 개까지만 예약할 수 있다(3개면 10.8달러). */
        private const val AFFORDABLE_JOB_COUNT = 2
        private const val CALL_MILLIS = 300L
        private const val POLL_MILLIS = 50L
        private const val WAIT_SECONDS = 30L
        private const val EVENT_COUNT = 20
        private const val EVENT_SPACING_HOURS = 4L
        private const val SAMPLE_TEXT = "오늘 정말 고마웠어 덕분에 힘이 났어"
        private const val CACHE_KEY = "같은 채점 입력"
        private val UNFINISHED = setOf("QUEUED", "ANALYZING")
        private const val PROMPT = "채점 프롬프트 1판"
        private const val DIMENSION_COUNT = 4
        private val DIMENSION_SCORE = BigDecimal("100")
        private val USAGE = ModelUsage(4_000, 90, "end_turn")
        private val ESTIMATE = ModelUsage(40_000, 250, "ESTIMATE")

        /** 입력 단가 0.3달러 × 1,200만 토큰 = 3.6달러. */
        private val QUARTER_BUDGET_PLUS = ModelUsage(12_000_000, 0, "ESTIMATE")
        private val OPEN_TIME: Instant = Instant.parse("2099-11-01T00:00:00Z")
    }
}
