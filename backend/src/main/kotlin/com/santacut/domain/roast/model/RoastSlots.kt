package com.santacut.domain.roast.model

import com.santacut.domain.analysis.model.GiftVerdict
import com.santacut.domain.analysis.model.ScoreComponents
import com.santacut.domain.analysis.model.ScoreDimension
import java.math.BigDecimal

/**
 * 서버가 정한 근거 카드 수. 작가 입력에 실어 그만큼만 쓰게 하고, 검증은 kind별 개수가 이 수와 같은지 본다.
 * 선물 덱은 선물 받는 이유([reason]) 최대 세 장과 점수 깎인 말([cut]) 한두 장, 석탄 덱은 점수 깎인 말 최대 세 장과 살아남은 말([keep]) 한 장이다.
 * 선물 덱도 놀림이 빠지지 않게 깎인 말을 늘 한 장 이상 둔다. 근거 카드는 [MAX_CARDS]장을 넘지 않아 덱이 여덟 장을 넘지 않는다.
 * 작가에게 보내기 전에 본인 말 줄 수와 방향별 칩 수로 줄인다([limitedTo]). 방향이 맞는 칩이 없는 자리는 만들지 않는다.
 */
data class RoastSlots(val reason: Int, val cut: Int, val keep: Int) {
    fun countOf(kind: EvidenceKind): Int = when (kind) {
        EvidenceKind.REASON -> reason
        EvidenceKind.CUT -> cut
        EvidenceKind.KEEP -> keep
    }

    fun total(): Int = reason + cut + keep

    /** 이미 채운 근거 [taken]을 kind별로 뺀 남은 수. 0 아래로 내려가지 않는다. */
    fun remainingAfter(taken: RoastEvidences): RoastSlots = RoastSlots(
        (reason - taken.countOf(EvidenceKind.REASON)).coerceAtLeast(0),
        (cut - taken.countOf(EvidenceKind.CUT)).coerceAtLeast(0),
        (keep - taken.countOf(EvidenceKind.KEEP)).coerceAtLeast(0),
    )

    /**
     * 쓸 수 있는 재료로 줄인 수. 선물 받는 이유와 살아남은 말은 본인 말 [lineCount]줄 안에서만 쓰고, 칩은 방향별 수 [supply] 안에서 나눈다.
     * 종류마다 방향이 맞는 칩(이유·살아남은 말은 좋은 쪽, 깎인 말은 나쁜 쪽)을 먼저 배정하고, 어느 쪽으로도 쓰는 칩은 모자란 자리에 나눠 준다.
     * 선물 덱은 이유, 석탄 덱은 깎인 말(주 카드)에 먼저 나눈다. 선물 덱은 칩이 둘 이상이면 깎인 말 몫으로 한 장을 먼저 남겨 놀림이 사라지지 않게 한다.
     * 나쁜 쪽 칩 셋에 깎인 말 자리가 둘이면 둘 다 남고, 석탄 덱에 좋은 쪽 칩이 하나라도 있으면 살아남은 말 자리가 남는다.
     */
    fun limitedTo(lineCount: Int, supply: ChipSupply): RoastSlots =
        if (reason > 0) giftLimitedTo(minOf(reason, lineCount), supply) else coalLimitedTo(minOf(keep, lineCount), supply)

    private fun giftLimitedTo(reasonByLines: Int, supply: ChipSupply): RoastSlots {
        val reasons = minOf(reasonByLines, supply.good)
        val cuts = minOf(cut, supply.bad)
        val total = supply.good + supply.bad + supply.either
        val reserved = if (cuts == 0 && cut > 0 && supply.either > 0 && (total > 1 || reasonByLines == 0)) 1 else 0
        val spare = supply.either - reserved
        val moreReasons = minOf(reasonByLines - reasons, spare)
        val moreCuts = minOf(cut - cuts - reserved, spare - moreReasons)
        return RoastSlots(reasons + moreReasons, cuts + reserved + moreCuts, 0)
    }

    private fun coalLimitedTo(keepByLines: Int, supply: ChipSupply): RoastSlots {
        val cuts = minOf(cut, supply.bad)
        val keeps = minOf(keepByLines, supply.good)
        val moreCuts = minOf(cut - cuts, supply.either)
        val moreKeeps = minOf(keepByLines - keeps, supply.either - moreCuts)
        return RoastSlots(0, cuts + moreCuts, keeps + moreKeeps)
    }

    companion object {
        val NONE = RoastSlots(0, 0, 0)

        /** 근거 카드 상한. 판정·컷 표·결론 세 장과 합쳐 덱이 여덟 장을 넘지 않는다. */
        const val MAX_CARDS = 5
        private const val MAX_GIFT_REASONS = 3
        private const val MIN_GIFT_CUTS = 1
        private const val MAX_GIFT_CUTS = 2
        private const val MAX_COAL_CUTS = 3
        private const val COAL_KEEPS = 1
        private val CUT_POINTS: BigDecimal = BigDecimal(GiftVerdict.CUT_POINTS)

        /**
         * 판정 [verdict]과 항목 점수 [dimensions]로 정한다. 컷(65점) 이상 항목 수가 선물 받는 이유 수, 컷 아래 항목 수가 점수 깎인 말 수다.
         * 선물 덱은 이유가 적어도 한 장이고, 모든 항목이 컷 이상이어도 깎인 말이 한 장 있다(가장 낮은 항목이나 가장 튀는 칩으로 채운다).
         * 석탄 덱은 깎인 말이 적어도 한 장이다. 근거 카드가 [MAX_CARDS]장을 넘으면 이유를 줄인다.
         */
        fun of(verdict: GiftVerdict, dimensions: ScoreComponents): RoastSlots {
            val strong = dimensions.countAtLeast(CUT_POINTS)
            val weak = ScoreDimension.entries.size - strong
            if (verdict.passed) {
                val cuts = weak.coerceIn(MIN_GIFT_CUTS, MAX_GIFT_CUTS)
                return RoastSlots(strong.coerceIn(1, minOf(MAX_GIFT_REASONS, MAX_CARDS - cuts)), cuts, 0)
            }
            return RoastSlots(0, weak.coerceIn(1, MAX_COAL_CUTS), COAL_KEEPS)
        }
    }
}
