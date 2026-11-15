package com.santacut.application.facade

import com.santacut.ErrorCodeAssertions.assertErrorCode
import com.santacut.IntegrationTest
import com.santacut.TestParticipants.REGION_CODE
import com.santacut.TestParticipants.uniqueNickname
import com.santacut.application.command.CreateShareCommand
import com.santacut.application.command.MyResultCommand
import com.santacut.application.command.PublicShareCommand
import com.santacut.application.command.RankingPageCommand
import com.santacut.application.command.RegisterParticipantCommand
import com.santacut.application.command.RevokeShareCommand
import com.santacut.domain.exception.ErrorCode
import com.santacut.domain.model.AnalysisEvents
import com.santacut.domain.model.AnalysisJob
import com.santacut.domain.model.AnalysisPayload
import com.santacut.domain.model.AnalysisSubmission
import com.santacut.domain.model.DataSource
import com.santacut.domain.model.IdempotencyKey
import com.santacut.domain.model.Score
import com.santacut.domain.service.AssessmentService
import com.santacut.domain.service.RankingService
import com.santacut.domain.service.ShareService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal
import java.time.Clock
import java.util.UUID
import kotlin.test.assertEquals
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

        val rows = resultFacade.getRankings(RankingPageCommand(0))
        assertEquals(listOf(nickname), rows.map { it.nickname })
        assertEquals(listOf(myResult.eta), rows.map { it.eta })
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
    fun `순위표 범위를 벗어난 offset과 남의 공유 토큰은 거절한다`() {
        assertErrorCode(ErrorCode.INVALID_OFFSET) { resultFacade.getRankings(RankingPageCommand(-1)) }
        assertErrorCode(ErrorCode.SHARE_NOT_FOUND) {
            shareFacade.revokeShare(RevokeShareCommand(UUID.randomUUID(), "x".repeat(43)))
        }
        assertErrorCode(ErrorCode.SHARE_NOT_FOUND) { shareFacade.getPublicPage(PublicShareCommand("bad-token")) }
    }

    private fun recordAssessmentAndRebuildRanking(participantId: UUID) {
        val now = clock.instant()
        val job = AnalysisJob.enqueue(
            participantId,
            IdempotencyKey.from("key-" + UUID.randomUUID()),
            AnalysisSubmission(DataSource.KAKAO, AnalysisEvents(emptyList())),
            AnalysisPayload("{}"),
            now,
        )
        val score = Score.weighted(listOf(BigDecimal(80), BigDecimal(90), BigDecimal(70), BigDecimal(60)))
        transactionTemplate.executeWithoutResult {
            assessmentService.record(job, score, now)
            rankingService.rebuild(now, shareService.findAll())
        }
    }

    companion object {
        private const val EXPECTED_SCORE = 7600
    }
}
