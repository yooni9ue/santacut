package com.santacut.domain.dossier.model

/**
 * 사전 항목 번호별 횟수. 사전(입버릇, 따뜻한 말, 주제×막)을 메시지마다 세어 더할 때 쓴다.
 */
class EntryCounts(private val values: IntArray) {
    fun add(index: Int, count: Int) {
        values[index] += count
    }

    operator fun get(index: Int): Int = values[index]

    companion object {
        /** 모두 0인 [size]칸. */
        fun ofSize(size: Int): EntryCounts = EntryCounts(IntArray(size))
    }
}
