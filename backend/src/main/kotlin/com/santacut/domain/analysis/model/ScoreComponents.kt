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

    /** 소수 자릿수와 상관없이 모든 항목 점수가 0~100점인지. 수축한 정밀 점수를 검증한다. */
    fun isWithinBounds(): Boolean = values.all { score -> score >= BigDecimal.ZERO && score <= MAX_DIMENSION_SCORE }

    /** 소수 둘째 자리로 반올림한 항목 점수. */
    fun rounded(): ScoreComponents =
        ScoreComponents(values.map { score -> plain(score.setScale(MAX_DECIMAL_PLACES, RoundingMode.HALF_UP).stripTrailingZeros()) })

    /** [dimension] 항목 점수. 항목 수가 모자라는 예전 행이면 중립 점수다. */
    fun scoreOf(dimension: ScoreDimension): BigDecimal = values.getOrNull(dimension.ordinal) ?: DimensionScores.NEUTRAL_SCORE

    /** 점수 높은 순으로 줄 세운 항목. 같으면 [ScoreDimension] 순서(가중치 큰 쪽)가 먼저다. */
    fun rankedDimensions(): List<ScoreDimension> =
        ScoreDimension.entries.sortedWith(compareByDescending<ScoreDimension> { scoreOf(it) }.thenBy { it.ordinal })

    /** 점수가 [points] 이상인 항목 수. */
    fun countAtLeast(points: BigDecimal): Int = ScoreDimension.entries.count { dimension -> scoreOf(dimension) >= points }

    /** 가장 높은 항목과 두 번째 항목의 점수 차이. */
    fun leadGap(): BigDecimal {
        val (top, runnerUp) = rankedDimensions()
        return scoreOf(top) - scoreOf(runnerUp)
    }

    /** `공감 72 · 응원 65 · 존중 70 · 협력 58`처럼 항목 이름과 반올림한 정수 점수를 잇는다. */
    fun toBarText(): String = ScoreDimension.entries.joinToString(BAR_SEPARATOR) { dimension ->
        "${dimension.label} ${scoreOf(dimension).setScale(0, RoundingMode.HALF_UP).toPlainString()}"
    }

    /** 항목 점수의 단순 평균. */
    fun average(): BigDecimal =
        ScoreDimension.entries.fold(BigDecimal.ZERO) { sum, dimension -> sum + scoreOf(dimension) }
            .divide(BigDecimal(ScoreDimension.entries.size), MAX_DECIMAL_PLACES, RoundingMode.HALF_UP)

    /** `9E+1`처럼 지수 표기가 되지 않게 음수 자릿수를 0으로 맞춘다. */
    private fun plain(score: BigDecimal): BigDecimal = if (score.scale() < 0) score.setScale(0) else score

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
        private const val BAR_SEPARATOR = " · "
        private const val COLUMN_PREFIX = "["
        private const val COLUMN_SUFFIX = "]"
        private const val COLUMN_SEPARATOR = ","

        /** 평가 행에 JSON 숫자 배열(`[63.25,71,55,66]`)로 저장한 항목 점수를 읽는다. 숫자가 아닌 칸은 건너뛴다. */
        fun fromColumn(column: String): ScoreComponents = ScoreComponents(
            column.removePrefix(COLUMN_PREFIX).removeSuffix(COLUMN_SUFFIX).split(COLUMN_SEPARATOR)
                .mapNotNull { cell -> cell.trim().toBigDecimalOrNull() },
        )
    }
}
