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
    fun shrunkScores(shrinkage: ScoreShrinkage, selfTurns: Int): List<BigDecimal> =
        ScoreDimension.entries.map { dimension -> shrunkScoreOf(dimension, shrinkage, selfTurns) }

    /** 항목의 근거 줄 번호를 차례로 [finder]에 넘겨 처음 나온 한마디. 없으면 null이다. */
    fun quoteOf(dimension: ScoreDimension, finder: (Int) -> String?): String? =
        supporting(dimension).flatMap { reading -> reading.evidence }.firstNotNullOfOrNull(finder)

    private fun shrunkScoreOf(dimension: ScoreDimension, shrinkage: ScoreShrinkage, selfTurns: Int): BigDecimal {
        val supported = supporting(dimension)
        if (supported.isEmpty()) return DimensionScores.NEUTRAL_SCORE
        val totalWeight = supported.sumOf { reading -> reading.aggregationWeight() }
        val mean = supported.sumOf { reading -> reading.score * reading.aggregationWeight() }
            .divide(totalWeight, ScoreShrinkage.PRECISION)
        return shrinkage.shrink(mean, supported.maxOf { reading -> reading.opportunity }, selfTurns)
    }

    private fun supporting(dimension: ScoreDimension): List<DimensionReading> =
        values.filter { reading -> reading.dimension == dimension && reading.hasEvidence() }

    companion object {
        val EMPTY = DimensionReadings(emptyList())
    }
}
