package com.santacut.domain.analysis.model

import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DailyBudgetTest {
    @Test
    fun `예산 날짜는 서울 날짜로 나눈다`() {
        assertEquals("2026-11-15", DailyBudget.dayOf(Instant.parse("2026-11-15T14:59:59Z")))
        assertEquals("2026-11-16", DailyBudget.dayOf(Instant.parse("2026-11-15T15:00:00Z")))
    }

    @Test
    fun `호출할 때마다 하나씩 쓰고 상한에 닿으면 더 쓸 수 없다`() {
        val budget = DailyBudget.startOf("2026-11-15")
        assertTrue(budget.hasRemaining(2))
        budget.consume()
        assertTrue(budget.hasRemaining(2))
        budget.consume()
        assertEquals(2, budget.calls)
        assertFalse(budget.hasRemaining(2))
    }
}
