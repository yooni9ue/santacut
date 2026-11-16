package com.santacut.domain.analysis.model

/**
 * 받은 배지 하나와 그 낱말 횟수.
 */
data class EarnedBadge(
    val rule: BadgeRule,
    val count: Int,
) {
    fun label(): String = rule.label(count)

    /** 평가 행에 저장할 `코드:횟수`. */
    fun toCell(): String = "${rule.name}$CELL_SEPARATOR$count"

    companion object {
        private const val CELL_SEPARATOR = ":"

        /** 평가 행의 `코드:횟수`를 읽는다. 모르는 코드나 숫자가 아닌 횟수면 null이다. */
        fun fromCell(cell: String): EarnedBadge? {
            val rule = runCatching { BadgeRule.valueOf(cell.substringBefore(CELL_SEPARATOR)) }.getOrNull() ?: return null
            val count = cell.substringAfter(CELL_SEPARATOR).toIntOrNull() ?: return null
            return EarnedBadge(rule, count)
        }
    }
}
