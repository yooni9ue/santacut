package com.santacut.application.share.facade

import com.santacut.ErrorCodeAssertions.assertErrorCode
import com.santacut.IntegrationTest
import com.santacut.TestParticipants.REGION_CODE
import com.santacut.TestParticipants.uniqueNickname
import com.santacut.application.participant.command.RegisterParticipantCommand
import com.santacut.application.participant.facade.ParticipantFacade
import com.santacut.application.ranking.command.MyResultCommand
import com.santacut.application.ranking.command.RankingPageCommand
import com.santacut.application.ranking.facade.ResultFacade
import com.santacut.application.share.command.CreateShareCommand
import com.santacut.application.share.command.PublicShareCommand
import com.santacut.application.share.command.RevokeShareCommand
import com.santacut.domain.analysis.model.AnalysisEvents
import com.santacut.domain.analysis.model.AnalysisJob
import com.santacut.domain.analysis.model.AnalysisPayload
import com.santacut.domain.analysis.model.AnalysisSubmission
import com.santacut.domain.analysis.model.DataSource
import com.santacut.domain.analysis.model.IdempotencyKey
import com.santacut.domain.analysis.model.Score
import com.santacut.domain.analysis.service.AssessmentService
import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.ranking.service.RankingService
import com.santacut.domain.share.service.ShareService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal
import java.time.Clock
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
    private val shareService: ShareService,
    private val transactionTemplate: TransactionTemplate,
    private val clock: Clock,
) {
    @Test
    fun `평가가 순위에 반영되면 내 결과와 순위표를 보고 공유 링크를 만들고 취소할 수 있다`() {
        val nickname = uniqueNickname()
        val participantId = participantFacade.register(RegisterParticipantCommand(nickname, REGION_CODE)).id
        recordAssessmentAndRebuildRanking(participantId)
        assertRankedResult(participantId, nickname)
        assertShareLifecycle(participantId, nickname)
    }

    private fun assertRankedResult(participantId: UUID, nickname: String) {
        val myResult = resultFacade.getMyResult(MyResultCommand(participantId))
        assertEquals(EXPECTED_SCORE, myResult.scoreHundredths)
        assertEquals("선물 배송 출발", myResult.verdict)
        assertEquals(1, myResult.deliverySequence)
        assertEquals(PREVIEW, myResult.stage)
        assertFalse(myResult.rankingPending)

        val board = resultFacade.getRankings(RankingPageCommand(null))
        assertEquals(nickname, board.rows.map { it.nickname }.first())
        assertEquals(myResult.eta, board.rows.map { it.eta }.first())
        assertEquals(myResult.computedAt, board.computedAt)
        assertEquals(board.rows.map { it.nickname }.size.toLong(), board.participantCount)
        assertEquals(PREVIEW, board.stage)
        assertNull(board.nextCursor)
    }

    private fun assertShareLifecycle(participantId: UUID, nickname: String) {
        val link = shareFacade.createShare(CreateShareCommand(participantId))
        assertEquals("http://localhost:8080/s/${link.token}", link.url)
        assertEquals(link.token, shareFacade.createShare(CreateShareCommand(participantId)).token)
        assertTrue(shareFacade.getPublicPage(PublicShareCommand(link.token)).html.contains(nickname))
        assertTrue(shareFacade.getPublicImage(PublicShareCommand(link.token)).png.isNotEmpty())

        shareFacade.revokeShare(RevokeShareCommand(participantId, link.token))
        assertErrorCode(ErrorCode.SHARE_NOT_FOUND) { shareFacade.getPublicPage(PublicShareCommand(link.token)) }
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
    fun `이전 응답이 준 값이 아닌 cursor와 남의 공유 토큰은 거절한다`() {
        assertErrorCode(ErrorCode.INVALID_CURSOR) { resultFacade.getRankings(RankingPageCommand("-1")) }
        assertErrorCode(ErrorCode.SHARE_NOT_FOUND) {
            shareFacade.revokeShare(RevokeShareCommand(UUID.randomUUID(), "x".repeat(43)))
        }
        assertErrorCode(ErrorCode.SHARE_NOT_FOUND) { shareFacade.getPublicPage(PublicShareCommand("bad-token")) }
    }

    private fun recordAssessmentAndRebuildRanking(participantId: UUID) =
        recordAssessment(participantId, HIGH_SCORES, rebuild = true)

    private fun recordAssessment(participantId: UUID, scores: List<Int>, rebuild: Boolean) {
        val now = clock.instant()
        val job = AnalysisJob.enqueue(
            participantId,
            IdempotencyKey.from("key-" + UUID.randomUUID()),
            AnalysisSubmission(DataSource.KAKAO, AnalysisEvents(emptyList())),
            AnalysisPayload("{}"),
            now,
        )
        val score = Score.weighted(scores.map { BigDecimal(it) })
        transactionTemplate.executeWithoutResult {
            assessmentService.record(job, score, now)
            if (rebuild) rankingService.rebuild(now, shareService.findAll())
        }
    }

    companion object {
        private const val EXPECTED_SCORE = 7600
        private val HIGH_SCORES = listOf(80, 90, 70, 60)

        /** 순위 계산 전 결과용 낮은 점수. 다른 테스트의 1위 참가자보다 늘 뒤에 선다. */
        private val LOW_SCORES = listOf(10, 10, 10, 10)
        private const val LOW_SCORE = 1000
        private const val PREVIEW = "PREVIEW"
    }
}
