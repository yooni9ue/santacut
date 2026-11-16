package com.santacut.domain.analysis.model

/**
 * 산타 유형별 참가자 수. 순위를 다시 계산할 때 한 번 세어 저장하고, 결과 화면은 저장한 값으로 비율만 낸다.
 * `HOT_COCOA_COURIER:3,FIREPLACE_KEEPER:5`처럼 저장한다.
 */
class ArchetypeCounts(private val values: Map<Archetype, Int>) {
    /** 유형이 [archetype]인 참가자 비율(%, 반올림). 참가자가 없으면 0이다. */
    fun sharePercentOf(archetype: Archetype): Int {
        val total = values.values.sum()
        if (total == 0) return 0
        return ((values[archetype] ?: 0) * PERCENT + total / 2) / total
    }

    /** 저장할 값. */
    fun toColumn(): String =
        values.entries.joinToString(COLUMN_SEPARATOR) { (archetype, count) -> "${archetype.name}$CELL_SEPARATOR$count" }

    companion object {
        private const val PERCENT = 100
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
