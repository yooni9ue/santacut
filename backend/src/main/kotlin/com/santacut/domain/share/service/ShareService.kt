package com.santacut.domain.share.service

import com.santacut.domain.analysis.model.Assessment
import com.santacut.domain.global.config.AppProperties
import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import com.santacut.domain.participant.model.Participant
import com.santacut.domain.ranking.model.RankingStage
import com.santacut.domain.ranking.model.ResultProfile
import com.santacut.domain.ranking.model.ResultSnapshot
import com.santacut.domain.share.model.Share
import com.santacut.domain.share.model.ShareLink
import com.santacut.domain.share.model.ShareToken
import com.santacut.infrastructure.share.render.ShareCard
import com.santacut.infrastructure.share.render.ShareImageRenderer
import com.santacut.infrastructure.share.render.SharePageRenderer
import com.santacut.infrastructure.share.repository.ShareRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class ShareService(
    private val shareRepository: ShareRepository,
    private val sharePageRenderer: SharePageRenderer,
    private val shareImageRenderer: ShareImageRenderer,
    private val appProperties: AppProperties,
) {
    /** 같은 스냅숏에 이미 만든 공유 링크가 있으면 다시 쓴다. */
    fun findActive(participantId: UUID, snapshot: ResultSnapshot): Share? =
        shareRepository.findByUserIdAndSnapshotId(participantId.toString(), snapshot.id)

    fun issue(
        participant: Participant,
        snapshot: ResultSnapshot,
        assessment: Assessment,
        stage: RankingStage,
        issuedAt: Instant,
        profile: ResultProfile,
        includeQuote: Boolean,
    ): Share = shareRepository.save(Share.issue(participant, snapshot, assessment, stage, issuedAt, profile, includeQuote))

    /** 이미 만든 공유 링크에 본인 한마디를 싣거나 뺀다. 링크(토큰)는 그대로다. */
    fun chooseQuote(share: Share, assessment: Assessment, includeQuote: Boolean): Share {
        share.chooseQuote(assessment.quote, includeQuote)
        return shareRepository.save(share)
    }

    fun linkOf(share: Share): ShareLink = ShareLink.of(appProperties.publicOrigin, share)

    /** 공유 링크를 지운다. 공개 HTML·PNG 주소는 이후 404가 된다. */
    fun revoke(participantId: UUID, token: String) {
        val share = shareRepository.findByTokenAndUserId(token, participantId.toString())
            ?: throw SantaCutException(ErrorCode.SHARE_NOT_FOUND)
        shareRepository.delete(share)
    }

    fun renderPage(token: ShareToken): String =
        sharePageRenderer.render(cardOf(findPublic(token)), appProperties.publicOrigin)

    fun renderImage(token: ShareToken): ByteArray = shareImageRenderer.render(cardOf(findPublic(token)))

    /** 공유 링크가 없거나 취소됐을 때 브라우저에 보여 줄 페이지. */
    fun renderInactivePage(): String = sharePageRenderer.renderInactive(appProperties.publicOrigin)

    /** 렌더러는 엔티티를 받지 않으므로 화면에 그릴 표시 값만 넘긴다. */
    private fun cardOf(share: Share): ShareCard = ShareCard(
        share.token, share.nickname, share.displayScore(), share.displayRank(), share.verdict, share.displayEta(),
        share.comment, share.displayComputedAt(), share.displayStage(), share.displaySharedAt(),
        share.displayArchetype(), share.displayBars(), share.displayHighlights(), share.quote,
    )

    private fun findPublic(token: ShareToken): Share =
        shareRepository.findByToken(token.value) ?: throw SantaCutException(ErrorCode.SHARE_NOT_FOUND)
}
