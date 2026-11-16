package com.santacut.domain.analysis.model

import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class ActiveDaysTest {
    private val first = LocalDate.of(2026, 11, 1)

    @Test
    fun `같은 날은 한 번만 센다`() {
        assertEquals(2, ActiveDays.of(listOf(first, first, first.plusDays(3))).size())
    }

    @Test
    fun `가장 긴 연속 일수는 순서와 상관없이 하루도 빠지지 않은 가장 긴 구간이다`() {
        val dates = listOf(5L, 1L, 2L, 0L, 6L, 7L, 8L, 7L).map { first.plusDays(it) }

        assertEquals(4, ActiveDays.of(dates).longestStreak())
    }

    @Test
    fun `하루만 있으면 1일, 하나도 없으면 0일이다`() {
        assertEquals(1, ActiveDays.of(listOf(first)).longestStreak())
        assertEquals(0, ActiveDays.of(emptyList()).longestStreak())
    }
}
