package com.santacut.domain.analysis.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * [ScoreDimension] 순서의 항목별 점수 목록.
 */
class ScoreComponents(private val values: List<BigDecimal>) {
    /** 모든 항목 점수가 0~100점이고 소수 둘째 자리까지인지. */
    fun isValid(): Boolean = values.all(::isValidDimensionScore)

    /**
     * 항목 점수 × 가중치(%)의 합을 반올림한 100분의 1점 단위 합계.
     * 가중치 합이 [ScoreDimension.TOTAL_WEIGHT]라서 곱셈의 합이 곧 100분의 1점 단위가 된다.
     */
    fun weightedHundredths(): Int = values.zip(ScoreDimension.entries)
        .fold(BigDecimal.ZERO) { sum, (score, dimension) -> sum + score * BigDecimal(dimension.weight) }
        .setScale(0, RoundingMode.HALF_UP)
        .intValueExact()

    /** 저장용 JSON으로 쓸 항목 점수의 방어 복사본. */
    fun toList(): List<BigDecimal> = values.toList()

    private fun isValidDimensionScore(score: BigDecimal): Boolean =
        score >= BigDecimal.ZERO && score <= MAX_DIMENSION_SCORE && score.scale() <= MAX_DECIMAL_PLACES

    companion object {
        /**
         * 항목 하나가 받을 수 있는 최고 점수(0~100점 척도).
         * 가중치 합계 [ScoreDimension.TOTAL_WEIGHT](100%)와는 숫자만 같고 뜻이 다르다.
         * 합계 상한은 이 값 × 가중치 합계 = 10000(100분의 1점 단위로 100.00점)이다.
         */
        private val MAX_DIMENSION_SCORE = BigDecimal(100)

        /** 항목 점수가 가질 수 있는 소수 자릿수. */
        private const val MAX_DECIMAL_PLACES = 2
    }
}
