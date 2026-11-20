package com.santacut.domain.dossier.model

/**
 * 칸마다의 개수(시간대 24칸, 달 12칸 등). 칸 번호는 0부터다. 메시지를 하나씩 받으며 칸을 늘린다.
 */
class Counts(private val values: IntArray) {
    fun add(index: Int) {
        values[index]++
    }

    fun toList(): List<Int> = values.toList()

    /** 모든 칸의 개수 합. */
    fun total(): Int = values.sum()

    /** [range] 칸의 개수 합. */
    fun totalIn(range: IntRange): Int = values.slice(range).sum()

    /** 가장 큰 칸. 모두 0이면 null이다. 같으면 앞 칸이다. */
    fun peakIndex(): Int? = values.indices.filter { index -> values[index] > 0 }.maxByOrNull { index -> values[index] }

    /** 0이 아닌 칸 중 가장 작은 칸. 모두 0이면 null이다. 같으면 앞 칸이다. */
    fun quietestIndex(): Int? = values.indices.filter { index -> values[index] > 0 }.minByOrNull { index -> values[index] }

    operator fun get(index: Int): Int = values[index]

    companion object {
        /** 모두 0인 [size]칸. */
        fun ofSize(size: Int): Counts = Counts(IntArray(size))
    }
}
