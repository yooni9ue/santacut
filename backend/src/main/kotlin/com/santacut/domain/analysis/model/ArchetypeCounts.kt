package com.santacut.domain.analysis.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 산타 유형별 참가자 수. 순위를 다시 계산할 때 한 번 세어 저장하고, 결과 화면은 저장한 값으로 비율만 낸다.
 * `HOT_COCOA_COURIER:3,FIREPLACE_KEEPER:5`처럼 저장한다.
 */
class ArchetypeCounts(private val values: Map<Archetype, Int>) {
    /**
     * 유형이 [archetype]인 참가자 비율(%, 반올림). 참가자가 [MIN_PARTICIPANTS_FOR_SHARE]명보다 적으면 null이다.
     * 참가자가 적으면 1명만으로 100%가 되는 것처럼 희귀도가 뜻이 없기 때문이다.
     */
    fun sharePercentOf(archetype: Archetype): Int? {
        val total = values.values.sum()
        if (total < MIN_PARTICIPANTS_FOR_SHARE) return null
        return BigDecimal(countOf(archetype) * PERCENT).divide(BigDecimal(total), INTEGER_SCALE, RoundingMode.HALF_UP).toInt()
    }

    private fun countOf(archetype: Archetype): Int = values.getOrDefault(archetype, NONE)

    /** 저장할 값. */
    fun toColumn(): String =
        values.entries.joinToString(COLUMN_SEPARATOR) { (archetype, count) -> "${archetype.name}$CELL_SEPARATOR$count" }

    companion object {
        /** 유형 비율을 보여 주는 가장 작은 참가자 수. 장난기 중앙값을 쓰기 시작하는 수와 같다. */
        const val MIN_PARTICIPANTS_FOR_SHARE = Assessments.MIN_PARTICIPANTS_FOR_MEDIAN
        private const val PERCENT = 100
        private const val INTEGER_SCALE = 0
        private const val NONE = 0
        private const val COLUMN_SEPARATOR = ","
        private const val CELL_SEPARATOR = ":"

        /** 저장한 값을 읽는다. 모르는 유형 코드와 숫자가 아닌 칸은 0명으로 본다. */
        fun fromColumn(column: String): ArchetypeCounts {
            val cells = column.split(COLUMN_SEPARATOR)
                .associate { cell -> cell.substringBefore(CELL_SEPARATOR) to cell.substringAfter(CELL_SEPARATOR) }
            return ArchetypeCounts(Archetype.entries.associateWith { archetype -> cells[archetype.name]?.toIntOrNull() ?: 0 })
        }
    }
}
