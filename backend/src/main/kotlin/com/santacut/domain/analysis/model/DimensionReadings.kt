package com.santacut.domain.analysis.model

import java.math.BigDecimal

/**
 * 모든 구간에서 읽은 항목별 점수 목록. 구간 점수를 항목마다 합치고 수축한다.
 */
class DimensionReadings(private val values: List<DimensionReading>) {
    operator fun plus(other: DimensionReadings): DimensionReadings = DimensionReadings(values + other.values)

    /** 어느 구간에도 근거가 없는 항목. 사전 평균(중립 점수)을 준다. */
    fun lowConfidence(): LowConfidenceDimensions =
        LowConfidenceDimensions(ScoreDimension.entries.filter { dimension -> supporting(dimension).isEmpty() })

    /**
     * 항목마다 근거가 있는 구간 점수를 무게([DimensionReading.aggregationWeight])로 평균하고,
     * 가장 큰 기회 수와 전체 본인 턴 수 [selfTurns]로 수축한 정밀 점수. [ScoreDimension] 순서다.
     */
    fun shrunkScores(shrinkage: ScoreShrinkage, selfTurns: Int): List<BigDecimal> {
        val prior = priorFor(shrinkage)
        return ScoreDimension.entries.map { dimension -> shrunkScoreOf(dimension, shrinkage, selfTurns) ?: prior }
    }

    /** 수축 전 항목별 평균. 근거 없는 항목은 중립 점수다. 유형을 고를 때 쓴다([Archetype.of]). [ScoreDimension] 순서다. */
    fun meanScores(): List<BigDecimal> = ScoreDimension.entries.map { dimension -> meanOf(supporting(dimension)) }

    /** 항목의 근거 줄 번호를 차례로 [finder]에 넘겨 처음 나온 한마디. 없으면 null이다. */
    fun quoteOf(dimension: ScoreDimension, finder: (Int) -> String?): String? =
        supporting(dimension).flatMap { reading -> reading.evidence }.firstNotNullOfOrNull(finder)

    /**
     * 근거 없는 항목에 줄 점수. [ScoreShrinkage.personalPrior]면 중립 점수 하나와 근거 있는 항목 평균들을 함께 평균한 값이고,
     * 아니면 중립 점수다. 대화가 짧아 그 행동을 보일 계기가 없었을 뿐인 항목을 본인의 다른 항목 수준에 가깝게 둔다.
     */
    private fun priorFor(shrinkage: ScoreShrinkage): BigDecimal {
        if (!shrinkage.personalPrior) return DimensionScores.NEUTRAL_SCORE
        val means = ScoreDimension.entries.map { dimension -> supporting(dimension) }.filter { supported -> supported.isNotEmpty() }.map { supported -> meanOf(supported) }
        return (means.fold(DimensionScores.NEUTRAL_SCORE, BigDecimal::add)).divide(BigDecimal(means.size + 1), ScoreShrinkage.PRECISION)
    }

    /** 근거가 있는 항목의 수축 점수. 근거가 없으면 null이다. */
    private fun shrunkScoreOf(dimension: ScoreDimension, shrinkage: ScoreShrinkage, selfTurns: Int): BigDecimal? {
        val supported = supporting(dimension)
        if (supported.isEmpty()) return null
        return shrinkage.shrink(meanOf(supported), supported.maxOf { reading -> reading.opportunity }, selfTurns)
    }

    /** 근거가 있는 구간 점수를 무게([DimensionReading.aggregationWeight])로 평균한다. 없으면 중립 점수다. */
    private fun meanOf(supported: List<DimensionReading>): BigDecimal {
        if (supported.isEmpty()) return DimensionScores.NEUTRAL_SCORE
        val totalWeight = supported.sumOf { reading -> reading.aggregationWeight() }
        return supported.sumOf { reading -> reading.score * reading.aggregationWeight() }.divide(totalWeight, ScoreShrinkage.PRECISION)
    }

    private fun supporting(dimension: ScoreDimension): List<DimensionReading> =
        values.filter { reading -> reading.dimension == dimension && reading.hasEvidence() }

    companion object {
        val EMPTY = DimensionReadings(emptyList())
    }
}
