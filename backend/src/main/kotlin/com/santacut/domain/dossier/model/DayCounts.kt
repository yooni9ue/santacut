package com.santacut.domain.dossier.model

import java.time.LocalDate

/**
 * 날짜별 본인 메시지 수. 한 해라 366칸을 넘지 않는다.
 */
class DayCounts(private val values: MutableMap<LocalDate, Int> = HashMap()) {
    fun add(day: LocalDate) {
        values.merge(day, 1, Int::plus)
    }

    /** 메시지가 하나라도 있는 날 수. */
    fun size(): Int = values.size

    /** 메시지가 가장 많은 날의 개수와 그 달. 같으면 이른 날이다. 비었으면 null이다. */
    fun busiest(): BusiestDay? = values.entries
        .sortedWith(compareByDescending<Map.Entry<LocalDate, Int>> { entry -> entry.value }.thenBy { entry -> entry.key })
        .firstOrNull()
        ?.let { entry -> BusiestDay(entry.value, entry.key.monthValue) }
}
