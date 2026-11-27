package com.santacut.domain.roast.model

import com.santacut.domain.analysis.model.GiftVerdict
import com.santacut.domain.analysis.model.ScoreComponents
import com.santacut.domain.analysis.model.ScoreDimension
import java.math.BigDecimal

/**
 * 서버가 정한 근거 카드 수. 작가 입력에 실어 그만큼만 쓰게 하고, 검증은 kind별 개수가 이 수와 같은지 본다.
 * 선물 덱은 선물 받는 이유([reason]) 최대 세 장과 점수 깎인 말([cut]) 한두 장, 석탄 덱은 점수 깎인 말 최대 세 장과 살아남은 말([keep]) 한 장이다.
 * 선물 덱도 놀림이 빠지지 않게 깎인 말을 늘 한 장 이상 둔다. 근거 카드는 [MAX_CARDS]장을 넘지 않아 덱이 여덟 장을 넘지 않는다.
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
     * 쓸 수 있는 재료로 줄인 수. 선물 받는 이유와 살아남은 말은 본인 말 [lineCount]줄 안에서만, 모든 카드는 칩 후보 [chipCount]개 안에서만 쓴다.
     * 빈자리를 일반론으로 채우지 않으려는 것이다. 칩이 모자라면 덱의 주 카드(선물은 이유, 석탄은 깎인 말)를 먼저 남긴다.
     * 선물 덱은 칩이 두 개 이상이면 깎인 말 몫으로 칩 하나를 남겨, 이유가 칩을 다 가져가 놀림이 사라지지 않게 한다.
     */
    fun limitedTo(lineCount: Int, chipCount: Int): RoastSlots {
        val reasonByLines = minOf(reason, lineCount)
        val keepByLines = minOf(keep, (lineCount - reasonByLines).coerceAtLeast(0))
        if (reason > 0) {
            val reservedForCut = if (cut > 0 && chipCount > 1) 1 else 0
            val reasons = minOf(reasonByLines, chipCount - reservedForCut)
            return RoastSlots(reasons, minOf(cut, chipCount - reasons), 0)
        }
        val cuts = minOf(cut, chipCount)
        return RoastSlots(0, cuts, minOf(keepByLines, chipCount - cuts))
    }

    /** 종류마다 [supply]의 수(그 종류에 방향이 맞는 칩 수)를 넘지 않게 줄인 수. 방향이 맞지 않는 칩으로 빈자리를 채우지 않으려는 것이다. */
    fun cappedBy(supply: RoastSlots): RoastSlots = RoastSlots(minOf(reason, supply.reason), minOf(cut, supply.cut), minOf(keep, supply.keep))

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
