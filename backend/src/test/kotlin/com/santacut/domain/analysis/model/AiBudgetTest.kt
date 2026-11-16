package com.santacut.domain.analysis.model

import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 토큰 수로 비용을 어림하고 하루·한 달 한도를 판단하는지 확인한다.
 */
class AiBudgetTest {
    private val budget = AiBudget(BigDecimal("0.5"), BigDecimal("9"), BigDecimal("1"), BigDecimal("5"))

    @Test
    fun `비용은 입력 100만 토큰당 1달러, 출력 100만 토큰당 5달러로 어림한다`() {
        assertEquals(0, BigDecimal("0.0055").compareTo(budget.costUsd(5_000, 100)))
    }

    @Test
    fun `하루 사용량에 호출마다 토큰과 호출 수를 더한다`() {
        val day = AiUsageDay("2026-11-16")

        day.add(ModelUsage(5_000, 100, "end_turn"))
        day.add(ModelUsage(3_000, 50, "end_turn"))

        assertEquals(8_000, day.inputTokens)
        assertEquals(150, day.outputTokens)
        assertEquals(2, day.calls)
    }

    @Test
    fun `오늘 비용이 하루 한도에 닿으면 막는다`() {
        val atLimit = days(ModelUsage(500_000, 0, ""))
        val belowLimit = days(ModelUsage(499_999, 0, ""))

        assertTrue(budget.isExhausted(atLimit, atLimit))
        assertFalse(budget.isExhausted(belowLimit, belowLimit))
    }

    @Test
    fun `이번 달 비용이 한 달 한도에 닿으면 오늘 비용이 적어도 막는다`() {
        val today = days(ModelUsage(1_000, 0, ""))
        val thisMonth = days(ModelUsage(4_000_000, 1_000_000, ""))

        assertTrue(budget.isExhausted(today, thisMonth))
    }

    @Test
    fun `이번 달 비용이 한 달 한도 바로 아래면 막지 않는다`() {
        val today = days(ModelUsage(1_000, 0, ""))
        val thisMonth = days(ModelUsage(3_999_999, 1_000_000, ""))

        assertFalse(budget.isExhausted(today, thisMonth))
    }

    private fun days(usage: ModelUsage): AiUsageDays = AiUsageDays(listOf(AiUsageDay("2026-11-16").apply { add(usage) }))
}
