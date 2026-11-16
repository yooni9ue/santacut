package com.santacut.domain.analysis.model

/**
 * AI 모델이 [ScoreDimension] 순서로 돌려준 항목별 기회 수(n). 0 없음, 1 한두 번, 2 여러 번이다.
 * 빠졌거나 숫자가 아니면(예전 형식 응답) [UNKNOWN]으로 보고, 범위 밖 값은 0~2로 자른다.
 */
class DimensionOpportunities(private val values: List<Int?>) {
    /** [position]번째 항목의 기회 수. */
    fun at(position: Int): Int = values.getOrNull(position)?.coerceIn(NONE, SEVERAL) ?: UNKNOWN

    companion object {
        private const val NONE = 0
        private const val SEVERAL = 2

        /** 기회 수를 받지 못했을 때. 수축을 가장 약하게 하려고 '여러 번'으로 본다. */
        const val UNKNOWN = SEVERAL

        val MISSING = DimensionOpportunities(emptyList())
    }
}
