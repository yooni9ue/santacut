package com.santacut.domain.service

import com.santacut.domain.config.AppProperties
import com.santacut.domain.exception.ErrorCode
import com.santacut.domain.exception.SantaCutException
import com.santacut.domain.model.Assessment
import com.santacut.domain.model.Participant
import com.santacut.domain.model.ResultSnapshot
import com.santacut.domain.model.Share
import com.santacut.domain.model.ShareLink
import com.santacut.domain.model.ShareToken
import com.santacut.domain.model.Shares
import com.santacut.infrastructure.render.ShareCard
import com.santacut.infrastructure.render.ShareImageRenderer
import com.santacut.infrastructure.render.SharePageRenderer
import com.santacut.infrastructure.repository.ShareRepository
import org.springframework.stereotype.Service
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

    fun issue(participant: Participant, snapshot: ResultSnapshot, assessment: Assessment): Share =
        shareRepository.save(Share.issue(participant, snapshot, assessment))

    /** 지금 남아 있는 모든 공유 링크. 순위 스냅숏 정리에서 지우면 안 되는 스냅숏을 가려내는 데 쓴다. */
    fun findAll(): Shares = Shares(shareRepository.findAll())

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

    /** 렌더러는 엔티티를 받지 않으므로 화면에 그릴 표시 값만 넘긴다. */
    private fun cardOf(share: Share): ShareCard = ShareCard(
        share.token, share.nickname, share.displayScore(), share.rank,
        share.verdict, share.displayEta(), share.comment, share.displayComputedAt(),
    )

    private fun findPublic(token: ShareToken): Share =
        shareRepository.findByToken(token.value) ?: throw SantaCutException(ErrorCode.SHARE_NOT_FOUND)
}
