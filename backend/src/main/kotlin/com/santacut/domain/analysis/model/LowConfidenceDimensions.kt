package com.santacut.domain.analysis.model

/**
 * 근거가 부족해 중립 점수([DimensionScores.NEUTRAL_SCORE])를 준 채점 항목. [ScoreDimension] 순서다.
 * 평가 행에는 항목 키를 쉼표로 이어 저장한다.
 */
data class LowConfidenceDimensions(private val values: List<ScoreDimension>) {
    /** 모든 항목이 근거 부족인지. 그러면 점수를 매길 내용이 없다고 본다. */
    fun coversAll(): Boolean = values.size == ScoreDimension.entries.size

    /** API로 내보낼 항목 키 목록. */
    fun toKeys(): List<String> = values.map { dimension -> dimension.key }

    /** 평가 행에 저장할 값. 근거 부족 항목이 없으면 빈 문자열이다. */
    fun toColumn(): String = toKeys().joinToString(COLUMN_SEPARATOR)

    companion object {
        private const val COLUMN_SEPARATOR = ","

        val NONE = LowConfidenceDimensions(emptyList())

        /** 평가 행에 저장한 값을 읽는다. 이 값이 생기기 전 평가(null)와 모르는 키는 근거 부족이 아닌 것으로 본다. */
        fun fromColumn(column: String?): LowConfidenceDimensions {
            val keys = column.orEmpty().split(COLUMN_SEPARATOR).toSet()
            return LowConfidenceDimensions(ScoreDimension.entries.filter { dimension -> dimension.key in keys })
        }
    }
}
