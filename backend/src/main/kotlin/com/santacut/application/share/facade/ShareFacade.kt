package com.santacut.application.share.facade

import com.santacut.application.global.Facade
import com.santacut.application.roast.result.RecapResult
import com.santacut.application.share.command.CreateShareCommand
import com.santacut.application.share.command.PublicShareCommand
import com.santacut.application.share.command.RevokeShareCommand
import com.santacut.application.share.result.ShareImageResult
import com.santacut.application.share.result.ShareLinkResult
import com.santacut.application.share.result.SharePageResult
import com.santacut.domain.analysis.model.Assessment
import com.santacut.domain.analysis.service.AssessmentService
import com.santacut.domain.participant.service.ParticipantService
import com.santacut.domain.ranking.model.ResultProfile
import com.santacut.domain.ranking.model.ResultSnapshot
import com.santacut.domain.ranking.service.RankingService
import com.santacut.domain.roast.service.RoastService
import com.santacut.domain.share.model.RecapDeck
import com.santacut.domain.share.model.Share
import com.santacut.domain.share.model.ShareToken
import com.santacut.domain.share.service.ShareService
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.util.UUID

/**
 * 공유 링크 발급·취소와 공개 페이지·이미지 조회.
 * 링크를 만들 때 서버가 그 평가의 리캡 카드 덱을 조립해 함께 고정한다. 클라이언트가 보낸 카드나 문구는 받지 않는다.
 */
@Facade
class ShareFacade(
    private val rankingService: RankingService,
    private val participantService: ParticipantService,
    private val assessmentService: AssessmentService,
    private val roastService: RoastService,
    private val shareService: ShareService,
    private val clock: Clock,
) {
    private val logger = LoggerFactory.getLogger(ShareFacade::class.java)

    /**
     * 최신 결과의 공유 링크를 만든다. 같은 결과로 이미 만든 링크가 있으면 다시 쓰고, 한마디를 실을지는 이번 요청을 따른다.
     * 다시 쓰는 링크에 리캡 카드 덱이 없으면 이때 한 번 채운다.
     */
    @Transactional
    fun createShare(command: CreateShareCommand): ShareLinkResult {
        val snapshot = rankingService.getLatestSnapshot(command.participantId)
        val share = shareService.findActive(command.participantId, snapshot)
            ?.let { reuseShare(it, snapshot, command.includeQuote) }
            ?: issueShare(command.participantId, snapshot, command.includeQuote)
        return ShareLinkResult.from(shareService.linkOf(share))
    }

    @Transactional
    fun revokeShare(command: RevokeShareCommand) = shareService.revoke(command.participantId, command.token)

    @Transactional(readOnly = true)
    fun getPublicPage(command: PublicShareCommand): SharePageResult =
        SharePageResult(shareService.renderPage(ShareToken.from(command.token)), shareService.pagePolicy())

    /** 공유 링크가 없거나 취소됐을 때 브라우저에 보여 줄 페이지. DB를 읽지 않는다. */
    fun getInactivePage(): SharePageResult = SharePageResult(shareService.renderInactivePage(), shareService.pagePolicy())

    @Transactional(readOnly = true)
    fun getPublicImage(command: PublicShareCommand): ShareImageResult =
        ShareImageResult(shareService.renderImage(ShareToken.from(command.token)))

    private fun reuseShare(share: Share, snapshot: ResultSnapshot, includeQuote: Boolean): Share {
        if (share.carriesQuote() == includeQuote && share.carriesRecap()) return share
        val assessment = assessmentService.get(snapshot.assessmentId)
        val quoted = if (share.carriesQuote() == includeQuote) share else shareService.chooseQuote(share, assessment, includeQuote)
        if (quoted.carriesRecap()) return quoted
        val deck = recapDeckOf(assessment, snapshot) ?: return quoted
        return shareService.attachRecap(quoted, deck)
    }

    private fun issueShare(participantId: UUID, snapshot: ResultSnapshot, includeQuote: Boolean): Share {
        val participant = participantService.get(participantId)
        val assessment = assessmentService.get(snapshot.assessmentId)
        val profile = rankingService.findProfile(assessment, snapshot)
        val now = clock.instant()
        val deck = recapDeckOf(assessment, snapshot, profile)
        return shareService.issue(participant, snapshot, assessment, rankingService.stageAt(now), now, profile, includeQuote, deck)
    }

    /**
     * [assessment]의 리캡 카드 덱. 로그인 사용자 리캡 응답(`RecapFacade`)과 같은 입력으로 만들어 카드 수와 순서가 같다.
     * 이 기능 전 평가라 팩폭 문장이 없거나 읽을 수 없으면 null이고, 링크는 카드 없이 만든다.
     * 꾸밈 값 [profile]을 이미 읽었으면 넘기고, 없으면 팩폭 문장이 있을 때만 읽는다.
     */
    private fun recapDeckOf(assessment: Assessment, snapshot: ResultSnapshot, profile: ResultProfile? = null): RecapDeck? {
        if (!roastService.exists(assessment.id)) return null
        return try {
            val content = roastService.get(assessment)
            RecapResult.of(assessment, snapshot, profile ?: rankingService.findProfile(assessment, snapshot), content).toDeck()
        } catch (exception: RuntimeException) {
            logger.warn("공유 링크에 실을 리캡 카드를 만들지 못해 카드 없이 둔다: {}", exception.javaClass.simpleName)
            null
        }
    }
}
