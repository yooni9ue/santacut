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
import com.santacut.application.ranking.command.MyResultCommand
import com.santacut.application.ranking.facade.ResultFacade
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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 짧은 대화를 [SAMPLES]번 따로 묻는 설정에서 분석 작업자가 호출마다 사용량을 기록하고, 항목별 중앙값을 평가로 저장하는지 확인한다.
 * 호출은 가상 스레드에서 동시에 불리고, 한 호출이 실패해도 이미 끝난 호출의 사용량은 남아야 한다.
 */
@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:sqlite:./build/worker-sampling-test.db",
        "app.season-year=2099",
        "santacut.ai.poll-delay-ms=3600000",
        "santacut.upload.poll-delay-ms=3600000",
        "santacut.score.sampling.samples=${AnalysisWorkerSamplingFacadeTest.SAMPLES}",
    ],
)
@Import(WorkerTestClockConfiguration::class)
@MockitoBean(types = [ScoreClient::class])
class AnalysisWorkerSamplingFacadeTest @Autowired constructor(
    private val analysisWorkerFacade: AnalysisWorkerFacade,
    private val analysisFacade: AnalysisFacade,
    private val resultFacade: ResultFacade,
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
    fun `세 호출을 가상 스레드에서 동시에 부르고 호출마다 사용량을 기록한 뒤 항목별 중앙값을 저장한다`() {
        val started = CountDownLatch(SAMPLES)
        val virtualThreads = ConcurrentLinkedQueue<Boolean>()
        val calls = AtomicInteger()
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willAnswer {
            virtualThreads += Thread.currentThread().isVirtual
            started.countDown()
            started.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            answerOf(SPREAD_SCORES[calls.getAndIncrement()])
        }
        val sampled = submitAnalysis()
        analysisWorkerFacade.processNextJob()
        val sampledUsage = aiUsageDayRepository.findAll().single().inputTokens

        // 앞 스텁의 답을 다시 부르지 않도록 호출 없이 바꿔 끼운다.
        willReturn(answerOf(MEDIAN_SCORE)).given(scoreClient).requestAnswer(anyChunk(), anyString())
        val agreed = submitAnalysis()
        analysisWorkerFacade.processNextJob()

        assertEquals(List(SAMPLES) { true }, virtualThreads.toList())
        assertEquals(USAGE.inputTokens * SAMPLES, sampledUsage)
        assertEquals(scoreOf(agreed), scoreOf(sampled))
    }

    @Test
    fun `한 호출이 예외를 던지면 작업은 실패하지만 나머지 호출의 사용량은 기록한다`() {
        val calls = AtomicInteger()
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willAnswer {
            if (calls.getAndIncrement() == 0) throw IllegalStateException("AI 장애")
            answerOf(MEDIAN_SCORE)
        }
        val job = submitAnalysis()

        analysisWorkerFacade.processNextJob()

        assertEquals("ANALYSIS_FAILED", analysisFacade.getStatus(job).errorCode)
        assertEquals(USAGE.inputTokens * (SAMPLES - 1), aiUsageDayRepository.findAll().single().inputTokens)
    }

    @Test
    fun `세 답 중 하나가 범위를 벗어나도 나머지 둘의 중앙값으로 작업을 성공시킨다`() {
        val calls = AtomicInteger()
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willAnswer {
            if (calls.getAndIncrement() == 0) answerOf(OUT_OF_RANGE_SCORE) else answerOf(MEDIAN_SCORE)
        }
        val job = submitAnalysis()

        analysisWorkerFacade.processNextJob()

        assertEquals("SUCCEEDED", analysisFacade.getStatus(job).status)
        assertTrue(scoreOf(job) > 0)
    }

    private fun scoreOf(job: AnalysisJobCommand): Int = resultFacade.getMyResult(MyResultCommand(job.participantId)).scoreHundredths

    private fun submitAnalysis(): AnalysisJobCommand {
        val participantId = participantFacade.register(RegisterParticipantCommand(uniqueNickname(), REGION_CODE)).id
        val occurredAt = clock.instant().minus(Duration.ofDays(1))
        val events = AnalysisEventCommands(
            List(EVENT_COUNT) { index ->
                AnalysisEventCommand(occurredAt.minus(Duration.ofHours(EVENT_SPACING_HOURS * index)), "$SAMPLE_TEXT $index")
            },
        )
        val command = SubmitAnalysisCommand(participantId, "key-" + UUID.randomUUID(), DataSource.KAKAO.name, events)
        return AnalysisJobCommand(participantId, UUID.fromString(analysisFacade.submit(command).id))
    }

    /** 모든 항목이 [score]점이고 근거는 첫 표본 줄인 답. */
    private fun answerOf(score: Int): ScoreAnswer =
        ScoreAnswer.of(List(DIMENSION_COUNT) { BigDecimal(score) }, List(DIMENSION_COUNT) { listOf(1) }, "test", USAGE)

    /** Kotlin의 null 불가 매개변수에도 쓸 수 있도록 매처를 등록한 뒤 null 대신 빈 제출의 채점 입력을 돌려준다. */
    private fun anyDigest(): ScoringDigest =
        any(ScoringDigest::class.java) ?: AnalysisSubmission(DataSource.KAKAO, AnalysisEvents(emptyList())).toScoringDigest()

    /** 매처를 등록한 뒤 null 대신 빈 제출 전체를 한 구간으로 돌려준다. */
    private fun anyChunk(): ScoringChunk = any(ScoringChunk::class.java) ?: ScoringChunk(TranscriptTurns(emptyList()), 1, 0, 0)

    companion object {
        const val SAMPLES = 3
        private const val EVENT_COUNT = 20

        /** 대화문에서 한 줄로 이어지지 않게 이벤트 사이를 3시간보다 넓게 띄운다. */
        private const val EVENT_SPACING_HOURS = 4L
        private const val SAMPLE_TEXT = "오늘 정말 고마웠어 덕분에 힘이 났어"
        private const val CACHE_KEY = "같은 채점 입력"
        private const val PROMPT = "채점 프롬프트 1판"
        private const val DIMENSION_COUNT = 4
        private const val TIMEOUT_SECONDS = 5L

        /** 세 호출이 따로 낸 항목 점수. 중앙값은 [MEDIAN_SCORE]다. */
        private val SPREAD_SCORES = listOf(60, 100, 80)
        private const val MEDIAN_SCORE = 80
        private const val OUT_OF_RANGE_SCORE = 170
        private val USAGE = ModelUsage(4_000, 90, "end_turn")
        private val ESTIMATE = ModelUsage(40_000, 250, "ESTIMATE")
        private val OPEN_TIME: Instant = Instant.parse("2099-11-01T00:00:00Z")
    }
}
