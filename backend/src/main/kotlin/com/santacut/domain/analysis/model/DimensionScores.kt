package com.santacut.domain.analysis.model

import java.math.BigDecimal

/**
 * AI 모델이 [ScoreDimension] 순서로 돌려준 항목 점수 목록. 숫자가 아닌 항목은 null이다.
 */
class DimensionScores(private val values: List<BigDecimal?>) {
    /** 모든 항목의 점수가 숫자로 들어왔는지. */
    fun isComplete(): Boolean = values.size == ScoreDimension.entries.size && values.all { it != null }

    /** 숫자인 항목 점수만 남긴 목록. [isComplete]를 통과했다면 모든 항목이다. [lowConfidence] 항목은 [NEUTRAL_SCORE]로 바꾼다. */
    fun toComponents(lowConfidence: LowConfidenceDimensions): List<BigDecimal> =
        values.zip(ScoreDimension.entries).mapNotNull { (score, dimension) -> if (lowConfidence.contains(dimension)) NEUTRAL_SCORE else score }

    companion object {
        /** 근거가 부족한 항목에 주는 중립 점수. 채점 프롬프트가 말하는 평범한 대화 구간(50~65)의 가운데쯤이다. */
        val NEUTRAL_SCORE = BigDecimal(55)
    }
}
