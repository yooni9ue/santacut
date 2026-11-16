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
import com.santacut.domain.analysis.model.AnalysisJob
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
import com.santacut.domain.ranking.model.Season
import com.santacut.infrastructure.analysis.client.ScoreClient
import com.santacut.infrastructure.analysis.repository.AiUsageDayRepository
import com.santacut.infrastructure.analysis.repository.AnalysisJobRepository
import com.santacut.infrastructure.analysis.repository.AssessmentRepository
import com.santacut.infrastructure.prompt.repository.PromptRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.BDDMockito.given
import org.mockito.Mockito.never
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
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 분석 작업자가 AI 점수 클라이언트를 바꿔 끼운 상태에서 성공, 실패, 연속 처리, 시즌 마감을 처리하는지 확인한다.
 * AI 호출마다 DB의 최신 채점 프롬프트를 쓰는지, 같은 채점 입력의 재제출에 지난 결과를 다시 쓰는지,
 * 호출 전 차단, 제공자 차단, 앱 안의 비용 가드도 확인한다.
 * 대기열 전체를 보는 작업자라 다른 통합 테스트와 섞이지 않게 `build/worker-test.db`를 따로 쓰고 테스트마다 비운다.
 * 작업자를 멈추게 하는 테스트는 멈춤 상태가 앱 메모리에 남으므로 끝난 뒤 컨텍스트를 새로 띄운다.
 */
@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:sqlite:./build/worker-test.db",
        "app.season-year=${AnalysisWorkerFacadeTest.SEASON_YEAR}",
        "santacut.ai.poll-delay-ms=3600000",
        "santacut.upload.poll-delay-ms=3600000",
        // 호출 수와 사용량을 세는 검증은 한 번 묻는 예전 흐름 기준이다. 중앙값 반복은 ScoringChunksTest·ScoreAnswersTest가 본다.
        "santacut.score.sampling.samples=1",
    ],
)
@Import(WorkerTestClockConfiguration::class)
@MockitoBean(types = [ScoreClient::class])
class AnalysisWorkerFacadeTest @Autowired constructor(
    private val analysisWorkerFacade: AnalysisWorkerFacade,
    private val analysisFacade: AnalysisFacade,
    private val resultFacade: ResultFacade,
    private val participantFacade: ParticipantFacade,
    private val scoreClient: ScoreClient,
    private val clock: MutableClock,
    private val analysisJobRepository: AnalysisJobRepository,
    private val promptRepository: PromptRepository,
    private val assessmentRepository: AssessmentRepository,
    private val aiUsageDayRepository: AiUsageDayRepository,
    private val aiBudgetService: AiBudgetService,
) {
    private val seasonClose: Instant = Season(SEASON_YEAR).deliveryStart().toInstant()

    @BeforeEach
    fun setUp() {
        analysisJobRepository.deleteAll()
        aiUsageDayRepository.deleteAll()
        promptRepository.deleteAll()
        promptRepository.save(Prompt.initial(PromptName.SCORE, FIRST_PROMPT, OPEN_TIME))
        clock.moveTo(OPEN_TIME)
        aiBudgetService.recordSuccess()
        given(scoreClient.isEnabled()).willReturn(true)
        given(scoreClient.cacheKey(anyDigest(), anyInt())).willReturn(CACHE_KEY)
        given(scoreClient.estimateUsage(anyChunk(), anyString())).willReturn(ESTIMATE)
    }

    @Test
    fun `점수를 받으면 평가를 저장하고 순위를 다시 계산해 작업을 성공으로 끝낸다`() {
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willReturn(fullAnswer())
        val job = submitAnalysis()

        analysisWorkerFacade.processNextJob()

        val status = analysisFacade.getStatus(job)
        assertEquals("SUCCEEDED", status.status)
        assertNotNull(status.assessmentId)
        val outcome = resultFacade.getMyResult(MyResultCommand(job.participantId))
        assertEquals(EXPECTED_SCORE_HUNDREDTHS, outcome.scoreHundredths)
        assertEquals(1, outcome.rank)
    }

    @Test
    fun `점수 산정 중 예외가 나면 작업을 ANALYSIS_FAILED로 끝낸다`() {
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willThrow(IllegalStateException("AI 장애"))
        val job = submitAnalysis()

        analysisWorkerFacade.processNextJob()

        val status = analysisFacade.getStatus(job)
        assertEquals("FAILED", status.status)
        assertEquals("ANALYSIS_FAILED", status.errorCode)
        assertNull(status.assessmentId)
        assertNotNull(status.finishedAt)
    }

    @Test
    fun `여러 작업을 연달아 채점한다`() {
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willReturn(fullAnswer())
        val jobs = List(CONSECUTIVE_JOB_COUNT) { submitAnalysis() }

        repeat(CONSECUTIVE_JOB_COUNT) { analysisWorkerFacade.processNextJob() }

        assertEquals(List(CONSECUTIVE_JOB_COUNT) { "SUCCEEDED" }, jobs.map { analysisFacade.getStatus(it).status })
    }

    @Test
    fun `AI가 꺼져 있어도 24시간 넘게 기다린 작업은 EXPIRED로 끝내고 AI는 부르지 않는다`() {
        val job = submitAnalysis()
        given(scoreClient.isEnabled()).willReturn(false)
        clock.moveTo(OPEN_TIME.plus(AnalysisJob.QUEUE_EXPIRY).plusSeconds(1))

        analysisWorkerFacade.processNextJob()

        val status = analysisFacade.getStatus(job)
        assertEquals("FAILED", status.status)
        assertEquals("EXPIRED", status.errorCode)
        verify(scoreClient, never()).requestAnswer(anyChunk(), anyString())
    }

    @Test
    fun `점수를 받은 사이에 시즌이 마감되면 평가를 저장하지 않고 SEASON_FROZEN으로 끝낸다`() {
        clock.moveTo(seasonClose.minus(Duration.ofMinutes(1)))
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willAnswer {
            clock.moveTo(seasonClose)
            fullAnswer()
        }
        val job = submitAnalysis()

        analysisWorkerFacade.processNextJob()

        val status = analysisFacade.getStatus(job)
        assertEquals("FAILED", status.status)
        assertEquals("SEASON_FROZEN", status.errorCode)
        assertNull(status.assessmentId)
    }

    @Test
    fun `AI를 부를 때마다 채점 프롬프트의 최신 판을 보내고 평가에 그 판 번호를 남긴다`() {
        val first = checkNotNull(promptRepository.findFirstByNameOrderByVersionDesc(PromptName.SCORE.value))
        val latest = promptRepository.save(first.next(LATEST_PROMPT, OPEN_TIME))
        given(scoreClient.requestAnswer(anyChunk(), eq(LATEST_PROMPT) ?: LATEST_PROMPT)).willReturn(fullAnswer())
        val job = submitAnalysis()

        analysisWorkerFacade.processNextJob()

        assertEquals("SUCCEEDED", analysisFacade.getStatus(job).status)
        val assessment = assessmentRepository.findFirstByUserIdOrderByCreatedAtDescIdDesc(job.participantId.toString())
        assertEquals(latest.version, assessment?.promptVersion)
    }

    @Test
    fun `채점 프롬프트가 하나도 없으면 AI를 부르지 않고 작업을 ANALYSIS_FAILED로 끝낸다`() {
        promptRepository.deleteAll()
        val job = submitAnalysis()

        analysisWorkerFacade.processNextJob()

        val status = analysisFacade.getStatus(job)
        assertEquals("FAILED", status.status)
        assertEquals("ANALYSIS_FAILED", status.errorCode)
        verify(scoreClient, never()).requestAnswer(anyChunk(), anyString())
    }

    @Test
    fun `표본이 너무 짧으면 AI를 부르지 않고 근거 부족으로 끝낸다`() {
        val job = submitAnalysis(text = SHORT_TEXT)

        analysisWorkerFacade.processNextJob()

        val status = analysisFacade.getStatus(job)
        assertEquals("FAILED", status.status)
        assertEquals("INSUFFICIENT_EVIDENCE", status.errorCode)
        verify(scoreClient, never()).requestAnswer(anyChunk(), anyString())
    }

    @Test
    fun `같은 참가자가 같은 채점 입력으로 다시 내면 AI를 다시 부르지 않고 지난 점수를 새 평가로 옮겨 적는다`() {
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willReturn(fullAnswer())
        val first = submitAnalysis()
        analysisWorkerFacade.processNextJob()
        val second = submitAnalysis(first.participantId)

        analysisWorkerFacade.processNextJob()

        val status = analysisFacade.getStatus(second)
        assertEquals("SUCCEEDED", status.status)
        assertNotEquals(analysisFacade.getStatus(first).assessmentId, status.assessmentId)
        assertEquals(EXPECTED_SCORE_HUNDREDTHS, resultFacade.getMyResult(MyResultCommand(second.participantId)).scoreHundredths)
        verify(scoreClient, times(1)).requestAnswer(anyChunk(), anyString())
    }

    @Test
    fun `같은 채점 입력이 근거 부족으로 끝났으면 다시 내도 AI 없이 근거 부족으로 끝낸다`() {
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willReturn(ScoreAnswer.insufficient(USAGE))
        val first = submitAnalysis()
        analysisWorkerFacade.processNextJob()
        val second = submitAnalysis(first.participantId)

        analysisWorkerFacade.processNextJob()

        assertEquals("INSUFFICIENT_EVIDENCE", analysisFacade.getStatus(first).errorCode)
        assertEquals("INSUFFICIENT_EVIDENCE", analysisFacade.getStatus(second).errorCode)
        verify(scoreClient, times(1)).requestAnswer(anyChunk(), anyString())
    }

    @Test
    fun `다른 참가자의 같은 채점 입력은 다시 쓰지 않는다`() {
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willReturn(fullAnswer())
        submitAnalysis()
        submitAnalysis()

        repeat(2) { analysisWorkerFacade.processNextJob() }

        verify(scoreClient, times(2)).requestAnswer(anyChunk(), anyString())
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun `제공자가 호출을 막으면 작업을 실패시키지 않고 대기열로 되돌린 뒤 멈춘다`() {
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willReturn(ScoreAnswer.providerUnavailable())
        val job = submitAnalysis()

        analysisWorkerFacade.processNextJob()
        analysisWorkerFacade.processNextJob()

        assertEquals("QUEUED", analysisFacade.getStatus(job).status)
        verify(scoreClient, times(1)).requestAnswer(anyChunk(), anyString())
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun `분석 오류가 3번 연달아 나면 멈춰서 다음 작업을 잡지 않는다`() {
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willThrow(IllegalStateException("AI 장애"))
        val jobs = List(MAX_CONSECUTIVE_FAILURES + 1) { submitAnalysis() }

        repeat(jobs.size) { analysisWorkerFacade.processNextJob() }

        assertEquals(List(MAX_CONSECUTIVE_FAILURES) { "ANALYSIS_FAILED" }, jobs.dropLast(1).map { analysisFacade.getStatus(it).errorCode })
        assertEquals("QUEUED", analysisFacade.getStatus(jobs.last()).status)
    }

    @Test
    fun `근거 부족은 연달아 나도 멈추지 않는다`() {
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willReturn(ScoreAnswer.insufficient(USAGE))
        val jobs = List(MAX_CONSECUTIVE_FAILURES + 1) { submitAnalysis() }

        repeat(jobs.size) { analysisWorkerFacade.processNextJob() }

        assertEquals(List(jobs.size) { "INSUFFICIENT_EVIDENCE" }, jobs.map { analysisFacade.getStatus(it).errorCode })
    }

    @Test
    fun `한 달 비용 한도에 닿으면 다음 작업을 잡지 않고 대기열에 둔다`() {
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willReturn(fullAnswer(EXPENSIVE_USAGE))
        val first = submitAnalysis()
        val second = submitAnalysis()

        repeat(2) { analysisWorkerFacade.processNextJob() }

        assertEquals("SUCCEEDED", analysisFacade.getStatus(first).status)
        assertEquals("QUEUED", analysisFacade.getStatus(second).status)
        assertEquals(EXPENSIVE_USAGE.inputTokens, aiUsageDayRepository.findAll().single().inputTokens)
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun `호출 전 어림 비용을 더하면 한도를 넘을 때는 AI를 부르지 않고 대기열로 되돌린 뒤 멈춘다`() {
        given(scoreClient.estimateUsage(anyChunk(), anyString())).willReturn(OVER_BUDGET_ESTIMATE)
        val job = submitAnalysis()

        analysisWorkerFacade.processNextJob()

        assertEquals("QUEUED", analysisFacade.getStatus(job).status)
        verify(scoreClient, never()).requestAnswer(anyChunk(), anyString())
        assertTrue(aiBudgetService.isBlocked(clock.instant()))
    }

    private fun submitAnalysis(participantId: UUID = registerParticipant(), text: String = SAMPLE_TEXT): AnalysisJobCommand {
        val occurredAt = clock.instant().minus(Duration.ofDays(1))
        val events = AnalysisEventCommands(List(EVENT_COUNT) { index -> AnalysisEventCommand(occurredAt.minus(Duration.ofHours(EVENT_SPACING_HOURS * index)), "$text $index") })
        val command = SubmitAnalysisCommand(participantId, "key-" + UUID.randomUUID(), DataSource.KAKAO.name, events)
        return AnalysisJobCommand(participantId, UUID.fromString(analysisFacade.submit(command).id))
    }

    private fun registerParticipant(): UUID =
        participantFacade.register(RegisterParticipantCommand(uniqueNickname(), REGION_CODE)).id

    /**
     * 모든 항목 100점, 근거는 첫 표본 줄. 기회 수가 없는 예전 형식이라 n=2로 보고,
     * 본인 턴이 20개 이상이라 근거 무게 w=2다. C=0.5로 수축하면 항목마다 (0.5×55 + 2×100) / 2.5 = 91점, 합계 9,100이다.
     */
    private fun fullAnswer(usage: ModelUsage = USAGE): ScoreAnswer =
        ScoreAnswer.of(List(DIMENSION_COUNT) { DIMENSION_SCORE }, List(DIMENSION_COUNT) { listOf(1) }, "test", usage)

    /** Kotlin의 null 불가 매개변수에도 쓸 수 있도록 매처를 등록한 뒤 null 대신 빈 제출의 채점 입력을 돌려준다. */
    private fun anyDigest(): ScoringDigest =
        any(ScoringDigest::class.java) ?: AnalysisSubmission(DataSource.KAKAO, AnalysisEvents(emptyList())).toScoringDigest()

    /** 매처를 등록한 뒤 null 대신 빈 제출 전체를 한 구간으로 돌려준다. */
    private fun anyChunk(): ScoringChunk = any(ScoringChunk::class.java) ?: ScoringChunk(TranscriptTurns(emptyList()), 1, 0, 0)

    companion object {
        const val SEASON_YEAR = 2099
        private const val CONSECUTIVE_JOB_COUNT = 6
        private const val EVENT_COUNT = 20

        /** 대화문에서 한 줄로 이어지지 않게 이벤트 사이를 3시간보다 넓게 띄운다. */
        private const val EVENT_SPACING_HOURS = 4L
        private const val SAMPLE_TEXT = "오늘 정말 고마웠어 덕분에 힘이 났어"
        private const val SHORT_TEXT = "고마워"
        private const val CACHE_KEY = "같은 채점 입력"
        private const val FIRST_PROMPT = "채점 프롬프트 1판"
        private const val LATEST_PROMPT = "채점 프롬프트 2판"
        private const val DIMENSION_COUNT = 4
        private const val EXPECTED_SCORE_HUNDREDTHS = 9500
        private val DIMENSION_SCORE = BigDecimal("100")
        private val USAGE = ModelUsage(4_000, 90, "end_turn")

        /** 입력 60만 토큰. 단가(100만 토큰당 1달러)로 0.6달러라 하루 한도 0.5달러를 넘는다. */
        /** 입력 단가 0.3달러 × 3,000만 토큰 = 한 달 한도 9달러. */
        private val EXPENSIVE_USAGE = ModelUsage(30_000_000, 0, "end_turn")
        private val ESTIMATE = ModelUsage(40_000, 250, "ESTIMATE")

        /** 한 번만으로 한 달 한도(9달러)를 넘는 어림 사용량. */
        private val OVER_BUDGET_ESTIMATE = ModelUsage(31_000_000, 250, "ESTIMATE")
        private val OPEN_TIME: Instant = Instant.parse("2099-11-01T00:00:00Z")

        private const val MAX_CONSECUTIVE_FAILURES = 3
    }
}
