package com.santacut.application.ranking.facade

import com.santacut.MutableClock
import com.santacut.TestParticipants.REGION_CODE
import com.santacut.TestParticipants.uniqueNickname
import com.santacut.application.analysis.command.AnalysisEventCommand
import com.santacut.application.analysis.command.AnalysisEventCommands
import com.santacut.application.analysis.command.SubmitAnalysisCommand
import com.santacut.application.analysis.facade.AnalysisFacade
import com.santacut.application.analysis.facade.AnalysisWorkerFacade
import com.santacut.application.analysis.facade.WorkerTestClockConfiguration
import com.santacut.application.participant.command.RegisterParticipantCommand
import com.santacut.application.participant.facade.ParticipantFacade
import com.santacut.application.ranking.command.MyResultCommand
import com.santacut.domain.analysis.model.AnalysisEvents
import com.santacut.domain.analysis.model.AnalysisJob
import com.santacut.domain.analysis.model.AnalysisJobStatus
import com.santacut.domain.analysis.model.AnalysisPayload
import com.santacut.domain.analysis.model.AnalysisSubmission
import com.santacut.domain.analysis.model.DataSource
import com.santacut.domain.analysis.model.EarnedBadges
import com.santacut.domain.analysis.model.IdempotencyKey
import com.santacut.domain.analysis.model.LowConfidenceDimensions
import com.santacut.domain.analysis.model.ModelUsage
import com.santacut.domain.analysis.model.Score
import com.santacut.domain.analysis.model.ScoreAnswer
import com.santacut.domain.analysis.model.ScoredAssessment
import com.santacut.domain.analysis.model.ScoringChunk
import com.santacut.domain.analysis.model.ScoringDigest
import com.santacut.domain.analysis.model.TranscriptTurns
import com.santacut.domain.analysis.service.AiBudgetService
import com.santacut.domain.analysis.service.AssessmentService
import com.santacut.domain.prompt.model.Prompt
import com.santacut.domain.prompt.model.PromptName
import com.santacut.domain.ranking.model.RankingRefresh
import com.santacut.domain.ranking.service.RankingService
import com.santacut.infrastructure.analysis.client.ScoreClient
import com.santacut.infrastructure.analysis.repository.AnalysisJobRepository
import com.santacut.infrastructure.prompt.repository.PromptRepository
import com.santacut.infrastructure.ranking.repository.RankingRefreshRepository
import com.santacut.presentation.analysis.controller.StreamParticipant
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.request
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 순위 다시 계산 요청이 커밋되면 예약 작업(여기서는 꺼 둠)을 기다리지 않고 곧 다시 계산하는지 확인한다.
 * 몰려 들어온 요청은 한 번의 계산으로 모이고, 계산이 실패해도 작업 결과는 그대로 남는다.
 * 대기열과 순위를 비우므로 `build/ranking-after-commit-test.db`를 따로 쓴다.
 */
@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:sqlite:./build/ranking-after-commit-test.db",
        "app.season-year=2099",
        "santacut.scheduling.enabled=false",
        "santacut.ranking.rebuild-after-change=true",
        "santacut.ranking.rebuild-debounce=${RankingRebuildAfterCommitTest.DEBOUNCE_MS}ms",
        "santacut.ranking.rebuild-min-interval=0ms",
    ],
)
@AutoConfigureMockMvc
@Import(WorkerTestClockConfiguration::class)
@MockitoBean(types = [ScoreClient::class])
@MockitoSpyBean(types = [RankingService::class])
class RankingRebuildAfterCommitTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val participantFacade: ParticipantFacade,
    private val analysisFacade: AnalysisFacade,
    private val analysisWorkerFacade: AnalysisWorkerFacade,
    private val resultFacade: ResultFacade,
    private val assessmentService: AssessmentService,
    private val aiBudgetService: AiBudgetService,
    private val scoreClient: ScoreClient,
    private val transactionTemplate: TransactionTemplate,
    private val jdbcTemplate: JdbcTemplate,
    private val analysisJobRepository: AnalysisJobRepository,
    private val promptRepository: PromptRepository,
    private val rankingRefreshRepository: RankingRefreshRepository,
    private val rankingService: RankingService,
    private val clock: MutableClock,
) {
    private val openedStreams = mutableListOf<MvcResult>()

    @BeforeEach
    fun setUp() {
        clock.moveTo(OPEN_TIME)
        awaitUntil { !isRebuildRequested() }
        analysisJobRepository.deleteAll()
        jdbcTemplate.update("DELETE FROM result_snapshots")
        promptRepository.deleteAll()
        promptRepository.save(Prompt.initial(PromptName.SCORE, PROMPT, OPEN_TIME))
        aiBudgetService.recordSuccess()
        given(scoreClient.isEnabled()).willReturn(true)
        given(scoreClient.cacheKey(anyDigest(), anyInt())).willReturn(CACHE_KEY)
        given(scoreClient.requestAnswer(anyChunk(), anyString())).willReturn(fullAnswer())
        given(scoreClient.estimateUsage(anyChunk(), anyString())).willReturn(USAGE)
        Mockito.clearInvocations(rankingService)
    }

    @AfterEach
    fun tearDown() {
        openedStreams.forEach { runCatching { it.request.asyncContext?.complete() } }
        Mockito.reset(rankingService)
    }

    @Test
    fun `작업이 성공하면 예약 작업 없이 곧 순위에 오르고 스트림이 SUCCEEDED를 받는다`() {
        val participant = register()
        val jobId = submit(participant.id)
        val stream = open(jobId, participant.session)

        analysisWorkerFacade.processNextJob()

        awaitUntil { stream.response.contentAsString.contains("\"status\":\"SUCCEEDED\"") }
        assertFalse(resultFacade.getMyResult(MyResultCommand(participant.id)).rankingPending)
        stream.getAsyncResult(ASYNC_WAIT_MS)
    }

    @Test
    fun `몰려 들어온 요청은 한 번의 계산으로 모이고 계산이 겹치지 않는다`() {
        val participants = List(BURST) { assessAndRequestRebuild() }

        awaitUntil { !isRebuildRequested() }
        Thread.sleep(DEBOUNCE_MS * 2)

        assertEquals(1, rebuildCount())
        participants.forEach { participantId -> assertFalse(resultFacade.getMyResult(MyResultCommand(participantId)).rankingPending) }
    }

    @Test
    fun `계산이 실패해도 작업은 성공으로 남고 요청 표시가 남아 다음 요청에서 다시 계산한다`() {
        Mockito.doThrow(IllegalStateException("순위 계산 실패")).`when`(rankingService).latestComputedAt()
        val participant = register()
        val jobId = submit(participant.id)

        analysisWorkerFacade.processNextJob()
        Thread.sleep(DEBOUNCE_MS * 3)

        assertEquals(AnalysisJobStatus.SUCCEEDED, analysisJobRepository.findById(jobId).orElseThrow().status)
        assertTrue(isRebuildRequested())
        Mockito.doCallRealMethod().`when`(rankingService).latestComputedAt()
        assessAndRequestRebuild()
        awaitUntil { !isRebuildRequested() }
        assertFalse(resultFacade.getMyResult(MyResultCommand(participant.id)).rankingPending)
    }

    /** 작업 성공 기록과 같이, 평가를 저장하고 같은 트랜잭션에서 순위 다시 계산을 요청한다. */
    private fun assessAndRequestRebuild(): UUID {
        val participantId = participantFacade.register(RegisterParticipantCommand(uniqueNickname(), REGION_CODE)).id
        val score = Score.shrunk(SCORES.map { BigDecimal(it) }, LowConfidenceDimensions.NONE, null)
        transactionTemplate.executeWithoutResult {
            assessmentService.record(emptyJob(participantId), ScoredAssessment(score, 1, null, EarnedBadges(emptyList())), clock.instant())
            rankingService.requestRebuild()
        }
        return participantId
    }

    private fun isRebuildRequested(): Boolean =
        rankingRefreshRepository.findById(RankingRefresh.ID).map(RankingRefresh::isDirty).orElse(false)

    private fun rebuildCount(): Int =
        Mockito.mockingDetails(rankingService).invocations.count { invocation -> invocation.method.name == REBUILD_METHOD }

    /** [condition]이 참이 될 때까지 기다린다. 시간 안에 참이 되지 않으면 실패다. */
    private fun awaitUntil(condition: () -> Boolean) {
        val deadline = System.nanoTime() + Duration.ofMillis(AWAIT_MS).toNanos()
        while (!condition()) {
            assertTrue(System.nanoTime() < deadline, "시간 안에 조건을 만족하지 않았다")
            Thread.sleep(POLL_MS)
        }
    }

    private fun open(jobId: String, session: Cookie): MvcResult =
        mockMvc.perform(get("/v1/analyses/$jobId/events").cookie(session).accept(MediaType.TEXT_EVENT_STREAM))
            .andExpect(request().asyncStarted())
            .andReturn()
            .also { openedStreams.add(it) }

    private fun register(): StreamParticipant {
        val registered = participantFacade.register(RegisterParticipantCommand(uniqueNickname(), REGION_CODE))
        return StreamParticipant(registered.id, Cookie(SESSION_COOKIE, registered.sessionToken))
    }

    private fun submit(participantId: UUID): String {
        val occurredAt = clock.instant().minus(Duration.ofDays(1))
        val events = AnalysisEventCommands(
            List(EVENT_COUNT) { index -> AnalysisEventCommand(occurredAt.minus(Duration.ofHours(EVENT_SPACING_HOURS * index)), "$SAMPLE_TEXT $index") },
        )
        return analysisFacade.submit(SubmitAnalysisCommand(participantId, "key-" + UUID.randomUUID(), SOURCE, events)).id
    }

    private fun emptyJob(participantId: UUID): AnalysisJob = AnalysisJob.enqueue(
        participantId,
        IdempotencyKey.from("key-" + UUID.randomUUID()),
        AnalysisSubmission(DataSource.KAKAO, AnalysisEvents(emptyList())),
        AnalysisPayload("{}"),
        clock.instant(),
    )

    private fun fullAnswer(): ScoreAnswer =
        ScoreAnswer.of(List(DIMENSION_COUNT) { BigDecimal(SCORE) }, List(DIMENSION_COUNT) { listOf(1) }, "test", USAGE)

    private fun anyChunk(): ScoringChunk = any(ScoringChunk::class.java) ?: ScoringChunk(TranscriptTurns(emptyList()), 1, 0, 0)

    private fun anyDigest(): ScoringDigest =
        any(ScoringDigest::class.java) ?: AnalysisSubmission(DataSource.KAKAO, AnalysisEvents(emptyList())).toScoringDigest()

    companion object {
        const val DEBOUNCE_MS = 300L
        private const val BURST = 5
        private const val AWAIT_MS = 10_000L
        private const val POLL_MS = 20L
        private const val ASYNC_WAIT_MS = 2_000L
        private const val REBUILD_METHOD = "rebuild"
        private const val SESSION_COOKIE = "sc_session"
        private const val SOURCE = "KAKAO"
        private const val EVENT_COUNT = 20
        private const val EVENT_SPACING_HOURS = 4L
        private const val SAMPLE_TEXT = "오늘 정말 고마웠어 덕분에 힘이 났어"
        private const val CACHE_KEY = "같은 채점 입력"
        private const val PROMPT = "채점 프롬프트"
        private const val DIMENSION_COUNT = 4
        private const val SCORE = 80
        private val SCORES = listOf(80, 70, 60, 50)
        private val USAGE = ModelUsage(4_000, 90, "end_turn")
        private val OPEN_TIME: Instant = Instant.parse("2099-11-01T00:00:00Z")
    }
}
