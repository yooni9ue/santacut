package com.santacut.application.share.facade

import com.santacut.ErrorCodeAssertions.assertErrorCode
import com.santacut.IntegrationTest
import com.santacut.TestParticipants.REGION_CODE
import com.santacut.TestParticipants.uniqueNickname
import com.santacut.application.participant.command.CurrentParticipantCommand
import com.santacut.application.participant.command.DeleteParticipantCommand
import com.santacut.application.participant.command.RegisterParticipantCommand
import com.santacut.application.participant.facade.ParticipantFacade
import com.santacut.application.ranking.command.MyResultCommand
import com.santacut.application.ranking.command.RankingPageCommand
import com.santacut.application.ranking.facade.ResultFacade
import com.santacut.application.ranking.result.MyRankingResult
import com.santacut.application.share.command.CreateShareCommand
import com.santacut.application.share.command.PublicShareCommand
import com.santacut.application.share.command.RevokeShareCommand
import com.santacut.domain.analysis.model.AnalysisEvents
import com.santacut.domain.analysis.model.AnalysisJob
import com.santacut.domain.analysis.model.AnalysisPayload
import com.santacut.domain.analysis.model.AnalysisSubmission
import com.santacut.domain.analysis.model.BadgeRule
import com.santacut.domain.analysis.model.DataSource
import com.santacut.domain.analysis.model.EarnedBadge
import com.santacut.domain.analysis.model.EarnedBadges
import com.santacut.domain.analysis.model.IdempotencyKey
import com.santacut.domain.analysis.model.LowConfidenceDimensions
import com.santacut.domain.analysis.model.Score
import com.santacut.domain.analysis.model.ScoredAssessment
import com.santacut.domain.analysis.service.AssessmentService
import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.model.SeoulTime
import com.santacut.domain.ranking.model.RankingCursor
import com.santacut.domain.ranking.service.RankingService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal
import org.springframework.jdbc.core.ConnectionCallback
import org.springframework.jdbc.core.JdbcTemplate
import java.time.Clock
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 평가가 순위에 반영된 뒤 내 결과 조회, 순위표, 공유 링크 생성·공개 조회·취소까지의 흐름.
 * 분석 작업자는 AI를 불러야 하므로 평가 저장과 순위 계산은 작업자가 쓰는 도메인 서비스로 직접 한다.
 */
@IntegrationTest
class ResultShareFlowTest @Autowired constructor(
    private val participantFacade: ParticipantFacade,
    private val resultFacade: ResultFacade,
    private val shareFacade: ShareFacade,
    private val assessmentService: AssessmentService,
    private val rankingService: RankingService,
    private val transactionTemplate: TransactionTemplate,
    private val clock: Clock,
    private val jdbcTemplate: JdbcTemplate,
) {
    @Test
    fun `평가가 순위에 반영되면 내 결과와 순위표를 보고 공유 링크를 만들고 취소할 수 있다`() {
        val nickname = uniqueNickname()
        val participantId = participantFacade.register(RegisterParticipantCommand(nickname, REGION_CODE)).id
        recordAssessmentAndRebuildRanking(participantId)
        val myResult = assertRankedMyResult(participantId)
        assertRankingBoardShows(myResult, nickname)
        assertShareLifecycle(participantId, nickname)
    }

    private fun assertRankedMyResult(participantId: UUID): MyRankingResult {
        val myResult = resultFacade.getMyResult(MyResultCommand(participantId))
        assertEquals(EXPECTED_SCORE, myResult.scoreHundredths)
        assertEquals("선물 배송 출발", myResult.verdict)
        assertEquals(1, myResult.deliverySequence)
        assertEquals(PREVIEW, myResult.stage)
        assertFalse(myResult.rankingPending)
        assertEquals("TREE_TOP_STAR", myResult.archetypeCode)
        assertEquals(listOf("공감", "응원", "존중", "협력"), myResult.dimensionBars.values.map { it.label })
        assertEquals("cooperation_sharing", myResult.weakestDimension.key)
        assertNull(myResult.topPercent)
        assertEquals(listOf("고마워 12회 · 감사 자판기"), myResult.badges)
        assertEquals(QUOTE, myResult.quote)
        return myResult
    }

    private fun assertRankingBoardShows(myResult: MyRankingResult, nickname: String) {
        val board = resultFacade.getRankings(RankingPageCommand(null))
        assertEquals(nickname, board.rows.map { it.nickname }.first())
        assertEquals(myResult.eta?.atZone(SeoulTime.ZONE)?.toLocalDate(), board.rows.map { it.etaDate }.first())
        assertEquals(myResult.computedAt, board.computedAt)
        assertEquals(board.rows.map { it.nickname }.size.toLong(), board.participantCount)
        assertEquals(PREVIEW, board.stage)
        assertNull(board.nextCursor)
    }

    private fun assertShareLifecycle(participantId: UUID, nickname: String) {
        val link = shareFacade.createShare(CreateShareCommand(participantId))
        assertEquals("http://localhost:8080/s/${link.token}", link.url)
        assertEquals(link.token, shareFacade.createShare(CreateShareCommand(participantId)).token)
        val page = shareFacade.getPublicPage(PublicShareCommand(link.token)).html
        assertTrue(page.contains(nickname) && page.contains("공유 당시 결과") && page.contains("미리보기"))
        assertTrue(page.contains("하트 먼저 누르는 애") && page.contains("감사 자판기") && page.contains("공감 80"))
        assertFalse(page.contains(QUOTE))
        assertTrue(shareFacade.getPublicImage(PublicShareCommand(link.token)).png.isNotEmpty())

        shareFacade.revokeShare(RevokeShareCommand(participantId, link.token))
        assertErrorCode(ErrorCode.SHARE_NOT_FOUND) { shareFacade.getPublicPage(PublicShareCommand(link.token)) }
        val quoted = shareFacade.createShare(CreateShareCommand(participantId, includeQuote = true))
        assertTrue(shareFacade.getPublicPage(PublicShareCommand(quoted.token)).html.contains(QUOTE))
        assertQuoteChoiceFollowsLatestRequest(participantId, quoted.token)
        shareFacade.revokeShare(RevokeShareCommand(participantId, quoted.token))
    }

    /** 같은 결과로 다시 공유하면 링크는 그대로 두고, 한마디를 실을지는 마지막 요청을 따른다. 카드 이미지도 바뀐다. */
    private fun assertQuoteChoiceFollowsLatestRequest(participantId: UUID, token: String) {
        val quotedImage = shareFacade.getPublicImage(PublicShareCommand(token)).png
        assertEquals(token, shareFacade.createShare(CreateShareCommand(participantId, includeQuote = false)).token)
        assertFalse(shareFacade.getPublicPage(PublicShareCommand(token)).html.contains(QUOTE))
        assertFalse(quotedImage.contentEquals(shareFacade.getPublicImage(PublicShareCommand(token)).png))
        assertEquals(token, shareFacade.createShare(CreateShareCommand(participantId, includeQuote = true)).token)
        assertTrue(shareFacade.getPublicPage(PublicShareCommand(token)).html.contains(QUOTE))
    }

    @Test
    fun `공유 당시 유형 코드가 지금은 없는 이름이어도 공유 페이지와 이미지는 유형 없이 뜬다`() {
        val nickname = uniqueNickname()
        val participantId = participantFacade.register(RegisterParticipantCommand(nickname, REGION_CODE)).id
        recordAssessmentAndRebuildRanking(participantId)
        val link = shareFacade.createShare(CreateShareCommand(participantId))
        storeRetiredArchetype(link.token)

        val page = shareFacade.getPublicPage(PublicShareCommand(link.token)).html
        assertTrue(page.contains(nickname) && !page.contains("하트 먼저 누르는 애"))
        assertTrue(shareFacade.getPublicImage(PublicShareCommand(link.token)).png.isNotEmpty())
        participantFacade.deleteMyData(DeleteParticipantCommand(participantId))
    }

    /** 예전 배포가 만든 enum CHECK 제약이 있어도 지금은 없는 유형 코드를 써 넣는다. 같은 연결에서만 제약을 잠시 끈다. */
    private fun storeRetiredArchetype(token: String) = jdbcTemplate.execute(
        ConnectionCallback { connection ->
            connection.createStatement().use { statement ->
                statement.execute("PRAGMA ignore_check_constraints = ON")
                statement.executeUpdate("UPDATE shares SET archetype = 'RETIRED_ARCHETYPE' WHERE token = '$token'")
                statement.execute("PRAGMA ignore_check_constraints = OFF")
            }
        },
    )

    @Test
    fun `내 데이터를 지우면 참가 정보, 순위, 공유 링크가 함께 사라진다`() {
        val nickname = uniqueNickname()
        val participantId = participantFacade.register(RegisterParticipantCommand(nickname, REGION_CODE)).id
        recordAssessmentAndRebuildRanking(participantId)
        val link = shareFacade.createShare(CreateShareCommand(participantId))

        participantFacade.deleteMyData(DeleteParticipantCommand(participantId))

        assertErrorCode(ErrorCode.PARTICIPANT_NOT_FOUND) {
            participantFacade.getCurrentParticipant(CurrentParticipantCommand(participantId))
        }
        assertErrorCode(ErrorCode.RESULT_NOT_FOUND) { resultFacade.getMyResult(MyResultCommand(participantId)) }
        assertErrorCode(ErrorCode.SHARE_NOT_FOUND) { shareFacade.getPublicPage(PublicShareCommand(link.token)) }
        assertFalse(resultFacade.getRankings(RankingPageCommand(null)).rows.map { it.nickname }.contains(nickname))
    }

    @Test
    fun `순위가 없는 참가자는 결과를 볼 수 없고 공유할 결과도 없다`() {
        val participantId = participantFacade.register(RegisterParticipantCommand(uniqueNickname(), REGION_CODE)).id
        assertErrorCode(ErrorCode.RESULT_NOT_FOUND) { resultFacade.getMyResult(MyResultCommand(participantId)) }
        assertErrorCode(ErrorCode.NO_RANKED_RESULT) { shareFacade.createShare(CreateShareCommand(participantId)) }
    }

    @Test
    fun `점수는 있지만 순위 계산 전이면 순위 없이 점수만 주고 반영 대기로 표시한다`() {
        val participantId = participantFacade.register(RegisterParticipantCommand(uniqueNickname(), REGION_CODE)).id
        recordAssessment(participantId, LOW_SCORES, rebuild = false)
        val myResult = resultFacade.getMyResult(MyResultCommand(participantId))
        assertEquals(LOW_SCORE, myResult.scoreHundredths)
        assertTrue(myResult.rankingPending)
        assertNull(myResult.rank)
        assertNull(myResult.eta)
        assertNull(myResult.computedAt)
    }

    @Test
    fun `순위 계산 뒤 새 평가가 아직 반영되지 않았으면 이전 스냅숏의 점수와 순위를 주고 반영 대기로 표시한다`() {
        val participantId = participantFacade.register(RegisterParticipantCommand(uniqueNickname(), REGION_CODE)).id
        val rankedAt = clock.instant()
        recordAssessment(participantId, MID_SCORES, rebuild = true, assessedAt = rankedAt)
        recordAssessment(participantId, LOW_SCORES, rebuild = false, assessedAt = rankedAt.plusSeconds(1))

        val myResult = resultFacade.getMyResult(MyResultCommand(participantId))

        assertEquals(MID_SCORE, myResult.scoreHundredths)
        assertNotNull(myResult.rank)
        assertNotNull(myResult.computedAt)
        assertTrue(myResult.rankingPending)
    }

    @Test
    fun `이전 페이지가 준 cursor로 그 배송 순서 다음 줄부터 읽는다`() {
        repeat(2) {
            val participantId = participantFacade.register(RegisterParticipantCommand(uniqueNickname(), REGION_CODE)).id
            recordAssessment(participantId, MID_SCORES, rebuild = true)
        }
        val firstPage = resultFacade.getRankings(RankingPageCommand(null))

        val afterFirstRow = RankingCursor.after(1, requireNotNull(firstPage.computedAt)).value()
        val secondPage = resultFacade.getRankings(RankingPageCommand(afterFirstRow))

        assertEquals(firstPage.rows.map { it.nickname }.drop(1), secondPage.rows.map { it.nickname })
        assertEquals(firstPage.participantCount, secondPage.participantCount)
        assertEquals(firstPage.computedAt, secondPage.computedAt)
        assertNull(secondPage.nextCursor)
    }

    @Test
    fun `페이지 사이에 순위를 다시 계산하면 이전 cursor는 거절하고 첫 페이지는 새 순위를 보여 준다`() {
        val participantId = participantFacade.register(RegisterParticipantCommand(uniqueNickname(), REGION_CODE)).id
        recordAssessment(participantId, MID_SCORES, rebuild = true)
        val firstPage = resultFacade.getRankings(RankingPageCommand(null))
        val firstComputedAt = requireNotNull(firstPage.computedAt)
        val staleCursor = RankingCursor.after(1, firstComputedAt).value()

        transactionTemplate.executeWithoutResult { rankingService.rebuild(firstComputedAt.plusSeconds(1)) }

        assertErrorCode(ErrorCode.RANKING_SNAPSHOT_CHANGED) { resultFacade.getRankings(RankingPageCommand(staleCursor)) }
        assertEquals(firstComputedAt.plusSeconds(1), resultFacade.getRankings(RankingPageCommand(null)).computedAt)
    }

    @Test
    fun `이전 응답이 준 값이 아닌 cursor와 남의 공유 토큰은 거절한다`() {
        assertErrorCode(ErrorCode.INVALID_CURSOR) { resultFacade.getRankings(RankingPageCommand("-1")) }
        assertErrorCode(ErrorCode.SHARE_NOT_FOUND) {
            shareFacade.revokeShare(RevokeShareCommand(UUID.randomUUID(), "x".repeat(43)))
        }
        assertErrorCode(ErrorCode.SHARE_NOT_FOUND) { shareFacade.getPublicPage(PublicShareCommand("bad-token")) }
    }

    private fun recordAssessmentAndRebuildRanking(participantId: UUID) =
        recordAssessment(participantId, HIGH_SCORES, rebuild = true)

    private fun recordAssessment(
        participantId: UUID,
        scores: List<Int>,
        rebuild: Boolean,
        assessedAt: Instant = clock.instant(),
    ) {
        val score = Score.shrunk(scores.map { BigDecimal(it) }, LowConfidenceDimensions.NONE, null)
        transactionTemplate.executeWithoutResult {
            assessmentService.record(emptyJob(participantId, assessedAt), ScoredAssessment(score, PROMPT_VERSION, QUOTE, BADGES), assessedAt)
            if (rebuild) rankingService.rebuild(assessedAt)
        }
    }

    /** 평가 저장에 필요한 작업. 이벤트와 본문은 비워 둔다. */
    private fun emptyJob(participantId: UUID, enqueuedAt: Instant): AnalysisJob =
        AnalysisJob.enqueue(
            participantId,
            IdempotencyKey.from("key-" + UUID.randomUUID()),
            AnalysisSubmission(DataSource.KAKAO, AnalysisEvents(emptyList())),
            AnalysisPayload("{}"),
            enqueuedAt,
        )

    companion object {
        private const val EXPECTED_SCORE = 7600
        private const val QUOTE = "덕분에 살았다"
        private val BADGES = EarnedBadges(listOf(EarnedBadge(BadgeRule.THANKS_MACHINE, 12)))
        private const val PROMPT_VERSION = 1
        private val HIGH_SCORES = listOf(80, 90, 70, 60)

        /** 순위 계산 전 결과용 낮은 점수. 다른 테스트의 1위 참가자보다 늘 뒤에 선다. */
        private val LOW_SCORES = listOf(10, 10, 10, 10)
        private const val LOW_SCORE = 1000

        /** 반영 대기·페이지 테스트용 중간 점수. 흐름 테스트의 1위 참가자보다 늘 뒤에 선다. */
        private val MID_SCORES = listOf(50, 50, 50, 50)
        private const val MID_SCORE = 5000

        private const val PREVIEW = "PREVIEW"
    }
}
