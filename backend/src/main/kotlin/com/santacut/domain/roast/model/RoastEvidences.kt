package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.santacut.domain.analysis.model.ScoreComponents
import com.santacut.domain.analysis.model.ScoreDimension
import com.santacut.domain.dossier.model.DossierFormat
import java.math.RoundingMode

/**
 * 근거 카드 목록. 저장 순서가 덱 순서(선물 받는 이유, 점수 깎인 말, 살아남은 말)다. JSON에서는 배열 하나로 읽고 쓴다.
 */
class RoastEvidences @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(
    private val values: List<RoastEvidence>,
) {
    @JsonValue
    fun toList(): List<RoastEvidence> = values.toList()

    fun size(): Int = values.size

    fun countOf(kind: EvidenceKind): Int = values.count { evidence -> evidence.kind == kind }

    /** 쓴 칩 key. */
    fun chipKeys(): Set<String> = values.map(RoastEvidence::chipKey).toSet()

    /** 이 카드에 [other]의 앞 [limit]장을 더해 덱 순서로 줄 세운다. [limit]가 0 이하면 더하지 않는다. */
    fun plus(other: RoastEvidences, limit: Int): RoastEvidences =
        RoastEvidences(values + other.values.take(limit.coerceAtLeast(0))).inDeckOrder()

    /** 대체 문장으로 채운 근거 카드 수. */
    fun fallbackCount(): Int = values.sumOf(RoastEvidence::fallbackCount)

    /** 결론 카드의 대표 근거로 쓸 첫 카드. 말풍선이 있는 카드, 고마워 칩이 아닌 카드 순으로 고른다. 없으면 null이다. */
    fun hero(): RoastEvidence? = values.firstOrNull { evidence -> evidence.quote != null }
        ?: values.firstOrNull { evidence -> evidence.chipKey != ThanksFactSpec.KEY }
        ?: values.firstOrNull()

    /**
     * 덱에 실을 근거 카드. 판정([passed])마다 종류별 상한으로 자르고, 덱이 다섯 장 아래로 내려가지 않게 항목 점수 통계 카드로 채운다.
     * 선물 덱은 이유와 깎인 말이 적어도 한 장씩(모든 항목이 컷 이상이어도 가장 낮은 항목으로 깎인 말을 채운다),
     * 석탄 덱은 깎인 말과 살아남은 말이 적어도 한 장씩이고, 근거 카드는 모두 [MIN_CARDS]장 이상이다.
     * 항목 점수 칩은 이유·살아남은 말이면 높은 항목부터, 깎인 말이면 낮은 항목부터 쓴다. 문구는 대체 문장([fallback])이다.
     */
    fun completedFor(passed: Boolean, dimensions: ScoreComponents, fallback: RoastFallback): RoastEvidences {
        val capped = RoastEvidences(EvidenceKind.entries.flatMap { kind -> values.filter { it.kind == kind }.take(limitOf(kind, passed)) })
        val required = if (passed) listOf(EvidenceKind.REASON, EvidenceKind.CUT) else listOf(EvidenceKind.CUT, EvidenceKind.KEEP)
        val withRequired = required.fold(capped) { cards, kind ->
            if (cards.countOf(kind) == 0) cards.withDimension(kind, dimensions, fallback) else cards
        }
        return withRequired.filledTo(if (passed) EvidenceKind.REASON else EvidenceKind.CUT, dimensions, fallback).inDeckOrder()
    }

    private tailrec fun filledTo(kind: EvidenceKind, dimensions: ScoreComponents, fallback: RoastFallback): RoastEvidences {
        if (values.size >= MIN_CARDS) return this
        val next = withDimension(kind, dimensions, fallback)
        if (next.size() == values.size) return this
        return next.filledTo(kind, dimensions, fallback)
    }

    /** [kind] 통계 카드 한 장을 더한다. 쓸 항목이 남지 않았으면 그대로다. */
    private fun withDimension(kind: EvidenceKind, dimensions: ScoreComponents, fallback: RoastFallback): RoastEvidences {
        val index = countOf(kind)
        val ranked = dimensions.rankedDimensions().let { order -> if (kind == EvidenceKind.CUT) order.reversed() else order }
        val dimension = ranked.getOrNull(index) ?: return this
        return RoastEvidences(values + fallback.evidenceOf(kind, index, dimensionFact(dimension, dimensions)))
    }

    private fun inDeckOrder(): RoastEvidences = RoastEvidences(values.sortedBy { evidence -> evidence.kind.ordinal })

    companion object {
        val EMPTY = RoastEvidences(emptyList())

        /** 덱이 다섯 장(판정, 컷 표, 결론 + 근거 두 장) 아래로 내려가지 않게 하는 근거 카드 최소 수. */
        const val MIN_CARDS = 2

        /** 항목 점수 칩 key 머리말. */
        const val DIMENSION_KEY_PREFIX = "dimension_"
        private const val MAX_GIFT_REASONS = 3
        private const val MAX_GIFT_CUTS = 2
        private const val MAX_COAL_CUTS = 3
        private const val MAX_COAL_KEEPS = 1
        private const val DIMENSION_CHIP_SUFFIX = " 점수"
        private const val POINT_UNIT = "점"

        /** 판정([passed])마다 [kind] 근거 카드 상한. 선물 덱은 이유 셋·깎인 말 둘, 석탄 덱은 깎인 말 셋·살아남은 말 하나다. */
        fun limitOf(kind: EvidenceKind, passed: Boolean): Int = when (kind) {
            EvidenceKind.REASON -> if (passed) MAX_GIFT_REASONS else 0
            EvidenceKind.CUT -> if (passed) MAX_GIFT_CUTS else MAX_COAL_CUTS
            EvidenceKind.KEEP -> if (passed) 0 else MAX_COAL_KEEPS
        }

        private fun dimensionFact(dimension: ScoreDimension, dimensions: ScoreComponents): RoastChipFact {
            val points = dimensions.scoreOf(dimension).setScale(0, RoundingMode.HALF_UP).toDouble()
            return RoastChipFact(DIMENSION_KEY_PREFIX + dimension.key, RoastChip(dimension.label + DIMENSION_CHIP_SUFFIX, DossierFormat.amount(points, POINT_UNIT)))
        }
    }
}
