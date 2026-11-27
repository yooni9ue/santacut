package com.santacut.application.roast.result

import com.santacut.domain.analysis.model.Assessment
import com.santacut.domain.analysis.model.GiftVerdict
import com.santacut.domain.ranking.model.ResultProfile
import com.santacut.domain.ranking.model.ResultSnapshot
import com.santacut.domain.roast.model.RoastContent
import com.santacut.domain.share.model.RecapDeck
import java.math.BigDecimal

/**
 * 리캡 카드 덱에 실을 값. 최신 평가와 그 평가의 순위 스냅숏, 꾸밈 값, 덱에 맞춘 팩폭 내용으로 만든다.
 *
 * @property skin 카드 스킨(GIFT, COAL)
 * @property score 총점(점, 소수 둘째 자리)
 * @property scoreText 총점 표시("73.73")
 * @property shareText 서버가 고정한 공유 문구. 등급마다 다르고 총점 표시가 들어간다.
 * @property top 모든 카드 상단 바 값(순위, 상위 비율)
 * @property cards 카드 덱(순서 고정, index는 0부터)
 */
data class RecapResult(
    val assessmentId: String,
    val skin: String,
    val score: BigDecimal,
    val scoreText: String,
    val shareText: String,
    val top: RecapTopResult,
    val cards: RecapCards,
) {
    /** 공유 링크에 고정할 카드 덱. [cards] 순서 그대로다. */
    fun toDeck(): RecapDeck = RecapDeck.of(skin, cards.toDeckCards(top, scoreText))

    companion object {
        private const val HUNDREDTHS_SCALE = 2

        /**
         * 최신 평가 [latest]와 그 평가의 순위 스냅숏 [snapshot](아직 순위에 오르지 않았으면 null), 꾸밈 값 [profile],
         * 덱에 맞춘 팩폭 내용 [content]([com.santacut.domain.roast.model.RoastContent.forDeck])로 만든다.
         * 순위는 순위 계산이 끝나면 참가자 수와 상관없이 늘 싣고, 최신 평가가 아직 순위에 반영되지 않았을 때만 null이다.
         * 상위 비율은 참가자가 [ResultProfile.MIN_PARTICIPANTS_FOR_TOP_PERCENT]명 아래면 null이다(작은 모집단의 '상위 100%'는 꼴찌로 읽힌다).
         */
        fun of(latest: Assessment, snapshot: ResultSnapshot?, profile: ResultProfile, content: RoastContent): RecapResult {
            val gift = GiftVerdict(latest.scoreHundredths)
            val rank = snapshot?.rank
            val top = RecapTopResult(rank, profile.topPercent, profile.participantCount.takeIf { rank != null }, snapshot == null)
            return RecapResult(
                latest.id, gift.skin(), BigDecimal.valueOf(latest.scoreHundredths.toLong(), HUNDREDTHS_SCALE), gift.scoreText(), gift.shareText(),
                top, RecapCards.of(gift, content, profile, snapshot?.eta),
            )
        }
    }
}
