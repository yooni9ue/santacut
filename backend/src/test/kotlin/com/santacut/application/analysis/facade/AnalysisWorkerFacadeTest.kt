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
import com.santacut.domain.analysis.model.DailyBudget
import com.santacut.domain.analysis.model.DataSource
import com.santacut.domain.analysis.model.ScoreAnswer
import com.santacut.domain.ranking.model.Season
import com.santacut.infrastructure.analysis.client.ScoreClient
import com.santacut.infrastructure.analysis.repository.AnalysisJobRepository
import com.santacut.infrastructure.analysis.repository.DailyBudgetRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * 분석 작업자가 AI 점수 클라이언트를 바꿔 끼운 상태에서 성공, 실패, 예산 소진, 시즌 마감을 처리하는지 확인한다.
 * 대기열 전체를 보는 작업자라 다른 통합 테스트와 섞이지 않게 `build/worker-test.db`를 따로 쓰고 테스트마다 비운다.
 */
@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:sqlite:./build/worker-test.db",
        "app.season-year=${AnalysisWorkerFacadeTest.SEASON_YEAR}",
        "santacut.ai.poll-delay-ms=3600000",
        "santacut.ai.daily-call-limit=${AnalysisWorkerFacadeTest.DAILY_CALL_LIMIT}",
    ],
)
@Import(WorkerTestClockConfiguration::class)
@MockitoBean(types = [ScoreClient::class])
class AnalysisWorkerFacadeTest @Autowired constructor(
    private val analysisWorkerFacade: AnalysisWorkerFacade,
    private val analysisFacade: AnalysisFacade,
    private val participantFacade: ParticipantFacade,
    private val scoreClient: ScoreClient,
    private val clock: MutableClock,
    private val analysisJobRepository: AnalysisJobRepository,
    private val dailyBudgetRepository: DailyBudgetRepository,
) {
    private val seasonClose: Instant = Season(SEASON_YEAR).deliveryStart().toInstant()

    @BeforeEach
    fun setUp() {
        analysisJobRepository.deleteAll()
        dailyBudgetRepository.deleteAll()
        clock.moveTo(OPEN_TIME)
        given(scoreClient.isEnabled()).willReturn(true)
    }

    @Test
    fun `점수를 받으면 평가를 저장하고 순위를 다시 계산해 작업을 성공으로 끝낸다`() {
        given(scoreClient.requestAnswer(anySubmission())).willReturn(fullAnswer())
        val job = submitAnalysis()

        analysisWorkerFacade.processNextJob()

        val status = analysisFacade.getStatus(job)
        assertEquals("SUCCEEDED", status.status)
        assertNotNull(status.assessmentId)
        val outcome = analysisFacade.getOutcome(job)
        assertEquals(EXPECTED_SCORE_HUNDREDTHS, outcome.scoreHundredths)
        assertEquals(1, outcome.rank)
    }

    @Test
    fun `점수 산정 중 예외가 나면 작업을 ANALYSIS_FAILED로 끝낸다`() {
        given(scoreClient.requestAnswer(anySubmission())).willThrow(IllegalStateException("AI 장애"))
        val job = submitAnalysis()

        analysisWorkerFacade.processNextJob()

        val status = analysisFacade.getStatus(job)
        assertEquals("FAILED", status.status)
        assertEquals("ANALYSIS_FAILED", status.errorCode)
        assertNull(status.assessmentId)
        assertNotNull(status.finishedAt)
    }

    @Test
    fun `오늘 AI 예산을 다 썼으면 작업을 건드리지 않고 AI도 부르지 않는다`() {
        dailyBudgetRepository.save(DailyBudget(DailyBudget.dayOf(OPEN_TIME), DAILY_CALL_LIMIT))
        val job = submitAnalysis()

        analysisWorkerFacade.processNextJob()

        assertEquals("QUEUED", analysisFacade.getStatus(job).status)
        verify(scoreClient, never()).requestAnswer(anySubmission())
    }

    @Test
    fun `점수를 받은 사이에 시즌이 마감되면 평가를 저장하지 않고 SEASON_FROZEN으로 끝낸다`() {
        clock.moveTo(seasonClose.minus(Duration.ofMinutes(1)))
        given(scoreClient.requestAnswer(anySubmission())).willAnswer {
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

    private fun submitAnalysis(): AnalysisJobCommand {
        val participantId = participantFacade.register(RegisterParticipantCommand(uniqueNickname(), REGION_CODE)).id
        val occurredAt = clock.instant().minus(Duration.ofDays(1))
        val events = AnalysisEventCommands(List(EVENT_COUNT) { index -> AnalysisEventCommand(occurredAt, "고마워 $index") })
        val command = SubmitAnalysisCommand(participantId, "key-" + UUID.randomUUID(), DataSource.KAKAO.name, events)
        return AnalysisJobCommand(participantId, UUID.fromString(analysisFacade.submit(command).id))
    }

    /** 모든 항목 80점, 근거는 첫 이벤트. 가중 합계는 8,000(80.00점)이다. */
    private fun fullAnswer(): ScoreAnswer =
        ScoreAnswer.of(List(DIMENSION_COUNT) { DIMENSION_SCORE }, List(DIMENSION_COUNT) { listOf(1) }, "test")

    /** Kotlin의 null 불가 매개변수에도 쓸 수 있도록 매처를 등록한 뒤 null 대신 빈 제출을 돌려준다. */
    private fun anySubmission(): AnalysisSubmission =
        any(AnalysisSubmission::class.java) ?: AnalysisSubmission(DataSource.KAKAO, AnalysisEvents(emptyList()))

    companion object {
        const val SEASON_YEAR = 2099
        const val DAILY_CALL_LIMIT = 5
        private const val EVENT_COUNT = 20
        private const val DIMENSION_COUNT = 4
        private const val EXPECTED_SCORE_HUNDREDTHS = 8000
        private val DIMENSION_SCORE = BigDecimal("80")
        private val OPEN_TIME: Instant = Instant.parse("2099-11-01T00:00:00Z")
    }
}
