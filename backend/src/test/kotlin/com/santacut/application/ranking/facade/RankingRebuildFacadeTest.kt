package com.santacut.application.ranking.facade

import com.santacut.MutableClock
import com.santacut.TestParticipants.REGION_CODE
import com.santacut.TestParticipants.uniqueNickname
import com.santacut.application.analysis.facade.WorkerTestClockConfiguration
import com.santacut.application.participant.command.RegisterParticipantCommand
import com.santacut.application.participant.facade.ParticipantFacade
import com.santacut.application.ranking.command.MyResultCommand
import com.santacut.application.ranking.command.RankingPageCommand
import com.santacut.domain.analysis.model.AnalysisEvents
import com.santacut.domain.analysis.model.AnalysisJob
import com.santacut.domain.analysis.model.AnalysisPayload
import com.santacut.domain.analysis.model.AnalysisSubmission
import com.santacut.domain.analysis.model.DataSource
import com.santacut.domain.analysis.model.EarnedBadges
import com.santacut.domain.analysis.model.IdempotencyKey
import com.santacut.domain.analysis.model.LowConfidenceDimensions
import com.santacut.domain.analysis.model.Score
import com.santacut.domain.analysis.model.ScoredAssessment
import com.santacut.domain.analysis.service.AssessmentService
import com.santacut.domain.ranking.model.Season
import com.santacut.domain.ranking.service.RankingService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 순위 다시 계산을 모아서 하는지, 시즌 마감 순간 마지막으로 다시 계산해 확정하는지 확인한다.
 * 확정 순위가 다른 테스트의 순위 계산을 막지 않도록 `build/ranking-rebuild-test.db`를 따로 쓰고 테스트마다 순위를 비운다.
 */
@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:sqlite:./build/ranking-rebuild-test.db",
        "app.season-year=${RankingRebuildFacadeTest.SEASON_YEAR}",
        "santacut.ai.poll-delay-ms=3600000",
        "santacut.upload.poll-delay-ms=3600000",
        "santacut.scheduling.enabled=false",
    ],
)
@Import(WorkerTestClockConfiguration::class)
class RankingRebuildFacadeTest @Autowired constructor(
    private val rankingRebuildFacade: RankingRebuildFacade,
    private val resultFacade: ResultFacade,
    private val participantFacade: ParticipantFacade,
    private val assessmentService: AssessmentService,
    private val rankingService: RankingService,
    private val transactionTemplate: TransactionTemplate,
    private val jdbcTemplate: JdbcTemplate,
    private val clock: MutableClock,
) {
    private val seasonClose = Season(SEASON_YEAR).deliveryStart().toInstant()

    @BeforeEach
    fun setUp() {
        clock.moveTo(seasonClose.minusSeconds(HOUR_SECONDS))
        rankingRebuildFacade.rebuildIfRequested()
        jdbcTemplate.update("DELETE FROM result_snapshots")
    }

    @Test
    fun `여러 변경은 요청만 쌓이고 예약 작업 한 번에 모두 순위에 오른다`() {
        val first = registerAndAssess()
        val second = registerAndAssess()
        assertTrue(resultFacade.getMyResult(MyResultCommand(first)).rankingPending)
        assertNull(rankingService.latestComputedAt())

        clock.moveTo(clock.instant().plusSeconds(1))
        rankingRebuildFacade.rebuildIfRequested()

        val rebuiltAt = rankingService.latestComputedAt()
        assertEquals(clock.instant(), rebuiltAt)
        assertFalse(resultFacade.getMyResult(MyResultCommand(first)).rankingPending)
        assertFalse(resultFacade.getMyResult(MyResultCommand(second)).rankingPending)
        clock.moveTo(clock.instant().plusSeconds(1))
        rankingRebuildFacade.rebuildIfRequested()
        assertEquals(rebuiltAt, rankingService.latestComputedAt())
    }

    @Test
    fun `시즌 마감 순간 마지막 요청을 반영해 확정하고 그 뒤로는 순위를 바꾸지 않는다`() {
        clock.moveTo(seasonClose.minusSeconds(1))
        val lastMinute = registerAndAssess()

        clock.moveTo(seasonClose)
        rankingRebuildFacade.rebuildAtSeasonFreeze()

        val board = resultFacade.getRankings(RankingPageCommand(null))
        assertEquals(seasonClose, board.computedAt)
        assertEquals(FINAL, board.stage)
        assertNotNull(resultFacade.getMyResult(MyResultCommand(lastMinute)).rank)
        transactionTemplate.executeWithoutResult { rankingService.requestRebuild() }
        clock.moveTo(seasonClose.plusSeconds(HOUR_SECONDS))
        rankingRebuildFacade.rebuildIfRequested()
        assertEquals(seasonClose, rankingService.latestComputedAt())
    }

    /** 참가자를 등록하고 지금 시각으로 평가를 저장한 뒤 순위 다시 계산만 요청한다. */
    private fun registerAndAssess(): UUID {
        val participantId = participantFacade.register(RegisterParticipantCommand(uniqueNickname(), REGION_CODE)).id
        val assessedAt = clock.instant()
        val score = Score.shrunk(SCORES.map { BigDecimal(it) }, LowConfidenceDimensions.NONE, null)
        transactionTemplate.executeWithoutResult {
            assessmentService.record(emptyJob(participantId, assessedAt), ScoredAssessment(score, 1, null, EarnedBadges(emptyList())), assessedAt)
            rankingService.requestRebuild()
        }
        return participantId
    }

    private fun emptyJob(participantId: UUID, enqueuedAt: Instant): AnalysisJob = AnalysisJob.enqueue(
        participantId,
        IdempotencyKey.from("key-" + UUID.randomUUID()),
        AnalysisSubmission(DataSource.KAKAO, AnalysisEvents(emptyList())),
        AnalysisPayload("{}"),
        enqueuedAt,
    )

    companion object {
        const val SEASON_YEAR = 2098
        private const val HOUR_SECONDS = 3600L
        private const val FINAL = "FINAL"
        private val SCORES = listOf(80, 70, 60, 50)
    }
}
