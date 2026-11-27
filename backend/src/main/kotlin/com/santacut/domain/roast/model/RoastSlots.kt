package com.santacut.domain.roast.model

import com.santacut.domain.analysis.model.GiftVerdict
import com.santacut.domain.analysis.model.ScoreComponents
import com.santacut.domain.analysis.model.ScoreDimension
import java.math.BigDecimal

/**
 * 서버가 정한 근거 카드 수. 작가 입력에 실어 그만큼만 쓰게 하고, 검증은 kind별 개수가 이 수와 같은지 본다.
 * 선물 덱은 선물 받는 이유([reason]) 최대 세 장과 점수 깎인 말([cut]) 최대 두 장, 석탄 덱은 점수 깎인 말 최대 세 장과 살아남은 말([keep]) 한 장이다.
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
     */
    fun limitedTo(lineCount: Int, chipCount: Int): RoastSlots {
        val reasonByLines = minOf(reason, lineCount)
        val keepByLines = minOf(keep, (lineCount - reasonByLines).coerceAtLeast(0))
        if (reason > 0) {
            val reasons = minOf(reasonByLines, chipCount)
            return RoastSlots(reasons, minOf(cut, chipCount - reasons), 0)
        }
        val cuts = minOf(cut, chipCount)
        return RoastSlots(0, cuts, minOf(keepByLines, chipCount - cuts))
    }

    companion object {
        val NONE = RoastSlots(0, 0, 0)
        private const val MAX_GIFT_REASONS = 3
        private const val MAX_GIFT_CUTS = 2
        private const val MAX_COAL_CUTS = 3
        private const val COAL_KEEPS = 1
        private val CUT_POINTS: BigDecimal = BigDecimal(GiftVerdict.CUT_POINTS)

        /**
         * 판정 [verdict]과 항목 점수 [dimensions]로 정한다. 컷(65점) 이상 항목 수가 선물 받는 이유 수, 컷 아래 항목 수가 점수 깎인 말 수다.
         * 선물 덱은 이유가 적어도 한 장, 석탄 덱은 깎인 말이 적어도 한 장이다.
         */
        fun of(verdict: GiftVerdict, dimensions: ScoreComponents): RoastSlots {
            val strong = dimensions.countAtLeast(CUT_POINTS)
            val weak = ScoreDimension.entries.size - strong
            if (verdict.passed) return RoastSlots(strong.coerceIn(1, MAX_GIFT_REASONS), weak.coerceAtMost(MAX_GIFT_CUTS), 0)
            return RoastSlots(0, weak.coerceIn(1, MAX_COAL_CUTS), COAL_KEEPS)
        }
    }
}
