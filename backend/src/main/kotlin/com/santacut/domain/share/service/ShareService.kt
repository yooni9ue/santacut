package com.santacut.domain.share.service

import com.santacut.domain.analysis.model.Assessment
import com.santacut.domain.global.config.AppProperties
import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import com.santacut.domain.participant.model.Participant
import com.santacut.domain.ranking.model.RankingStage
import com.santacut.domain.ranking.model.ResultProfile
import com.santacut.domain.ranking.model.ResultSnapshot
import com.santacut.domain.share.model.RecapDeck
import com.santacut.domain.share.model.Share
import com.santacut.domain.share.model.ShareLink
import com.santacut.domain.share.model.ShareToken
import com.santacut.infrastructure.share.render.ShareCard
import com.santacut.infrastructure.share.render.ShareDeck
import com.santacut.infrastructure.share.render.ShareImageRenderer
import com.santacut.infrastructure.share.render.SharePageRenderer
import com.santacut.infrastructure.share.repository.ShareRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.core.JacksonException
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.UUID

/**
 * 공유 링크 발급·취소와 공개 페이지·이미지 그리기. 로그에는 토큰과 카드 문구를 남기지 않는다.
 */
@Service
class ShareService(
    private val shareRepository: ShareRepository,
    private val sharePageRenderer: SharePageRenderer,
    private val shareImageRenderer: ShareImageRenderer,
    private val appProperties: AppProperties,
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(ShareService::class.java)

    /** 같은 스냅숏에 이미 만든 공유 링크가 있으면 다시 쓴다. */
    fun findActive(participantId: UUID, snapshot: ResultSnapshot): Share? =
        shareRepository.findByUserIdAndSnapshotId(participantId.toString(), snapshot.id)

    /** 공유 링크를 만든다. 리캡 카드 덱 [deck]은 팩폭 문장이 없던 평가면 null이고, 그 링크는 요약 화면으로 보인다. */
    fun issue(
        participant: Participant,
        snapshot: ResultSnapshot,
        assessment: Assessment,
        stage: RankingStage,
        issuedAt: Instant,
        profile: ResultProfile,
        includeQuote: Boolean,
        deck: RecapDeck?,
    ): Share = shareRepository.save(
        Share.issue(participant, snapshot, assessment, stage, issuedAt, profile, includeQuote, deck?.let(objectMapper::writeValueAsString)),
    )

    /** 카드 덱이 없는 기존 링크에 [deck]을 한 번 싣는다. 링크(토큰)는 그대로다. */
    fun attachRecap(share: Share, deck: RecapDeck): Share {
        share.attachRecap(objectMapper.writeValueAsString(deck))
        return shareRepository.save(share)
    }

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

    /** 카드 덱이 있는 링크는 카드 덱 페이지로, 없거나 읽을 수 없는 링크는 기존 요약 페이지로 그린다. */
    fun renderPage(token: ShareToken): String {
        val share = findPublic(token)
        val deck = deckOf(share) ?: return sharePageRenderer.render(cardOf(share), appProperties.publicOrigin)
        return sharePageRenderer.renderDeck(cardOf(share), deck, appProperties.publicOrigin)
    }

    /** 공유 페이지 응답(볼 수 없는 결과 페이지 포함)에 붙일 Content-Security-Policy. */
    fun pagePolicy(): String = sharePageRenderer.contentSecurityPolicy(appProperties.publicOrigin)

    fun renderImage(token: ShareToken): ByteArray = shareImageRenderer.render(cardOf(findPublic(token)))

    /** 공유 링크가 없거나 취소됐을 때 브라우저에 보여 줄 페이지. */
    fun renderInactivePage(): String = sharePageRenderer.renderInactive(appProperties.publicOrigin)

    /** 렌더러는 엔티티를 받지 않으므로 화면에 그릴 표시 값만 넘긴다. */
    private fun cardOf(share: Share): ShareCard = ShareCard(
        share.token, share.nickname, share.displayScore(), share.displayRank(), share.verdict, share.displayEta(),
        share.comment, share.displayComputedAt(), share.displayStage(), share.displaySharedAt(),
        share.displayArchetype(), share.displayBars(), share.displayHighlights(), share.quote,
    )

    /** 저장한 카드 덱. 없거나, 읽을 수 없거나, 지금 코드가 모르는 판이면 null이다. 1판과 2판 덱을 모두 읽는다([ShareDeckReader]). */
    private fun deckOf(share: Share): ShareDeck? {
        val json = share.recapJson ?: return null
        return try {
            ShareDeckReader.read(json, objectMapper)
        } catch (exception: JacksonException) {
            logger.warn("공유 리캡 카드 덱을 읽지 못해 요약 화면으로 보여 준다: {}", exception.javaClass.simpleName)
            null
        }
    }

    private fun findPublic(token: ShareToken): Share =
        shareRepository.findByToken(token.value) ?: throw SantaCutException(ErrorCode.SHARE_NOT_FOUND)
}
