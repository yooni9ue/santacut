package com.santacut.application.facade

import com.santacut.application.command.CreateShareCommand
import com.santacut.application.command.PublicShareCommand
import com.santacut.application.command.RevokeShareCommand
import com.santacut.application.result.ShareImageResult
import com.santacut.application.result.ShareLinkResult
import com.santacut.application.result.SharePageResult
import com.santacut.domain.exception.ErrorCode
import com.santacut.domain.exception.SantaCutException
import com.santacut.domain.model.ResultSnapshot
import com.santacut.domain.model.Share
import com.santacut.domain.model.ShareToken
import com.santacut.domain.service.AssessmentService
import com.santacut.domain.service.ParticipantService
import com.santacut.domain.service.RankingService
import com.santacut.domain.service.ShareService
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Facade
class ShareFacade(
    private val rankingService: RankingService,
    private val participantService: ParticipantService,
    private val assessmentService: AssessmentService,
    private val shareService: ShareService,
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

    @Transactional(readOnly = true)
    fun getPublicImage(command: PublicShareCommand): ShareImageResult =
        ShareImageResult(shareService.renderImage(ShareToken.from(command.token)))

    private fun issueShare(participantId: UUID, snapshot: ResultSnapshot): Share {
        val participant = participantService.get(participantId)
        val assessment = assessmentService.get(snapshot.assessmentId)
        return shareService.issue(participant, snapshot, assessment)
    }
}
