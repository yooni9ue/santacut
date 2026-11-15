package com.santacut.application.share.facade

import com.santacut.application.global.Facade
import com.santacut.application.share.command.CreateShareCommand
import com.santacut.application.share.command.PublicShareCommand
import com.santacut.application.share.command.RevokeShareCommand
import com.santacut.application.share.result.ShareImageResult
import com.santacut.application.share.result.ShareLinkResult
import com.santacut.application.share.result.SharePageResult
import com.santacut.domain.analysis.service.AssessmentService
import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import com.santacut.domain.participant.service.ParticipantService
import com.santacut.domain.ranking.model.ResultSnapshot
import com.santacut.domain.ranking.service.RankingService
import com.santacut.domain.share.model.Share
import com.santacut.domain.share.model.ShareToken
import com.santacut.domain.share.service.ShareService
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.util.UUID

@Facade
class ShareFacade(
    private val rankingService: RankingService,
    private val participantService: ParticipantService,
    private val assessmentService: AssessmentService,
    private val shareService: ShareService,
    private val clock: Clock,
) {
    /** 최신 결과의 공유 링크를 만든다. 같은 결과로 이미 만든 링크가 있으면 다시 쓴다. */
    @Transactional
    fun createShare(command: CreateShareCommand): ShareLinkResult {
        val snapshot = rankingService.findLatestSnapshot(command.participantId)
            ?: throw SantaCutException(ErrorCode.NO_RANKED_RESULT)
        val share = shareService.findActive(command.participantId, snapshot)
            ?: issueShare(command.participantId, snapshot)
        return ShareLinkResult.from(shareService.linkOf(share))
    }

    @Transactional
    fun revokeShare(command: RevokeShareCommand) = shareService.revoke(command.participantId, command.token)

    @Transactional(readOnly = true)
    fun getPublicPage(command: PublicShareCommand): SharePageResult =
        SharePageResult(shareService.renderPage(ShareToken.from(command.token)))

    /** 공유 링크가 없거나 취소됐을 때 브라우저에 보여 줄 페이지. DB를 읽지 않는다. */
    fun getInactivePage(): SharePageResult = SharePageResult(shareService.renderInactivePage())

    @Transactional(readOnly = true)
    fun getPublicImage(command: PublicShareCommand): ShareImageResult =
        ShareImageResult(shareService.renderImage(ShareToken.from(command.token)))

    private fun issueShare(participantId: UUID, snapshot: ResultSnapshot): Share {
        val participant = participantService.get(participantId)
        val assessment = assessmentService.get(snapshot.assessmentId)
        val now = clock.instant()
        return shareService.issue(participant, snapshot, assessment, rankingService.stageAt(now), now)
    }
}
