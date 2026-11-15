package com.santacut.domain.model

import java.math.BigDecimal

/**
 * AI 모델이 [ScoreDimension] 순서로 돌려준 항목 점수 목록. 숫자가 아닌 항목은 null이다.
 */
class DimensionScores(private val values: List<BigDecimal?>) {
    /** 모든 항목의 점수가 숫자로 들어왔는지. */
    fun isComplete(): Boolean = values.size == ScoreDimension.entries.size && values.all { it != null }

    /** 숫자인 항목 점수만 남긴 목록. [isComplete]를 통과했다면 모든 항목이다. */
    fun toComponents(): List<BigDecimal> = values.filterNotNull()
}
