package com.santacut.domain.dossier.model

/**
 * 칸마다의 개수(시간대 24칸, 달 12칸 등). 칸 번호는 0부터다.
 */
class Counts(private val values: List<Int>) {
    fun toList(): List<Int> = values.toList()

    fun total(): Int = values.sum()

    /** [range] 칸의 개수 합. */
    fun totalIn(range: IntRange): Int = values.slice(range).sum()

    /** 가장 큰 칸. 모두 0이면 null이다. 같으면 앞 칸이다. */
    fun peakIndex(): Int? = values.indices.filter { index -> values[index] > 0 }.maxByOrNull { index -> values[index] }

    /** 0이 아닌 칸 중 가장 작은 칸. 모두 0이면 null이다. 같으면 앞 칸이다. */
    fun quietestIndex(): Int? = values.indices.filter { index -> values[index] > 0 }.minByOrNull { index -> values[index] }

    /** 가장 큰 값. 비었으면 0이다. */
    fun max(): Int = values.maxOrNull() ?: 0

    operator fun get(index: Int): Int = values[index]

    fun size(): Int = values.size

    companion object {
        /** 칸 번호 목록 [indices]를 [size]칸으로 센다. */
        fun of(indices: List<Int>, size: Int): Counts {
            val counted = indices.groupingBy { index -> index }.eachCount()
            return Counts(List(size) { index -> counted[index] ?: 0 })
        }
    }
}
