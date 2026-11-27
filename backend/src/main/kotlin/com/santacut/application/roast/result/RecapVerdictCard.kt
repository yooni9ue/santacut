package com.santacut.application.roast.result

import com.santacut.domain.analysis.model.GiftVerdict
import com.santacut.domain.share.model.RecapDeckCard
import com.santacut.domain.share.model.RecapDeckKind
import java.math.BigDecimal
import java.time.Instant

/**
 * 판정 카드. 큰 글자(착한 아이, 나쁜 아이)와 점수·컷 거리 칩, 선물 도착일 칩, AI 한 줄을 싣는다.
 *
 * @property verdict 큰 글자. 선물이면 "착한 아이", 석탄이면 "나쁜 아이"다.
 * @property cutDistance 컷 거리 표시("+8.73", "-3.20")
 * @property cutDistanceValue 컷 거리 숫자(점)
 * @property grade 구간 이름
 * @property gradeCode 구간 코드([com.santacut.domain.analysis.model.GiftGrade] 이름)
 * @property verdictLine AI 한 줄(18자 이하, 옛 결과는 더 길 수 있다)
 * @property deliveryEta 선물 도착 예정 시각. 석탄이거나 순위 계산 전이면 null이다.
 */
data class RecapVerdictCard(
    override val index: Int,
    val verdict: String,
    val passed: Boolean,
    val score: BigDecimal,
    val cut: Int,
    val cutDistance: String,
    val cutDistanceValue: BigDecimal,
    val grade: String,
    val gradeCode: String,
    val verdictLine: String,
    val deliveryEta: Instant?,
) : RecapCard {
    override val kind: RecapCardKind = RecapCardKind.VERDICT

    override fun at(index: Int): RecapCard = copy(index = index)

    override fun toDeckCard(top: RecapTopResult, scoreText: String): RecapDeckCard = RecapDeckCard(
        index, RecapDeckKind.VERDICT, scoreText, top.rank, top.topPercent, verdict = verdict, passed = passed, grade = grade,
        cutDistance = cutDistance, verdictLine = verdictLine, deliveryEta = deliveryEta?.toString(),
    )

    companion object {
        private const val HUNDREDTHS_SCALE = 2

        /** 판정 [gift]와 AI 한 줄 [verdictLine], 도착 예정 시각 [eta](순위 계산 전이면 null)로 만든다. */
        fun of(gift: GiftVerdict, verdictLine: String, eta: Instant?): RecapVerdictCard = RecapVerdictCard(
            0, gift.childLabel(), gift.passed, BigDecimal.valueOf(gift.hundredths.toLong(), HUNDREDTHS_SCALE), GiftVerdict.CUT_POINTS,
            gift.cutDistanceText(), BigDecimal.valueOf(gift.cutDistanceHundredths().toLong(), HUNDREDTHS_SCALE), gift.grade().label,
            gift.grade().name, verdictLine, eta?.takeIf { gift.passed },
        )
    }
}
