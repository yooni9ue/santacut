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
    private val budget = AiBudget(BigDecimal("0.5"), BigDecimal("9"), SCORE_PRICE, ROAST_PRICE)

    @Test
    fun `채점 비용은 입력 100만 토큰당 1달러, 출력 100만 토큰당 5달러로 어림한다`() {
        assertEquals(0, BigDecimal("0.0055").compareTo(budget.costUsd(AiCallKind.SCORE, ModelUsage(5_000, 100, ""))))
    }

    @Test
    fun `캐시에서 읽은 입력 토큰은 캐시 단가로 센다`() {
        assertEquals(0, BigDecimal("0.0046").compareTo(budget.costUsd(AiCallKind.SCORE, ModelUsage(5_000, 100, "", 0, 1_000))))
    }

    @Test
    fun `작가 비용은 작가 모델의 입력, 캐시 읽기, 출력, 생각 단가로 따로 센다`() {
        val usage = ModelUsage(5_000, 100, "STOP", 50, 1_000)

        // (4,000 × 2 + 1,000 × 0.2 + 100 × 10 + 50 × 20) / 1,000,000
        assertEquals(0, BigDecimal("0.0102").compareTo(budget.costUsd(AiCallKind.ROAST, usage)))
        // (4,000 × 1 + 1,000 × 0.1 + 150 × 5) / 1,000,000
        assertEquals(0, BigDecimal("0.00485").compareTo(budget.costUsd(AiCallKind.SCORE, usage)))
    }

    @Test
    fun `하루 사용량은 작가 호출을 작가 칸에 따로 더하고 비용은 두 모델 단가로 센 합이다`() {
        val day = AiUsageDay("2026-11-16")

        day.add(ModelUsage(5_000, 100, "STOP"))
        day.add(ModelUsage(5_000, 100, "STOP", 50, 1_000), AiCallKind.ROAST)

        assertEquals(5_000, day.inputTokens)
        assertEquals(1, day.calls)
        assertEquals(5_000, day.roastInputTokens)
        assertEquals(1_000, day.roastCachedInputTokens)
        assertEquals(100, day.roastOutputTokens)
        assertEquals(50, day.roastThoughtTokens)
        assertEquals(1, day.roastCalls)
        assertEquals(0, BigDecimal("0.0157").compareTo(day.costUsd(budget)))
    }

    @Test
    fun `같은 어림 사용량이라도 작가 단가로 세면 더 빨리 한도를 넘는다`() {
        val today = days(ModelUsage(400_000, 0, ""))
        val estimate = ModelUsage(60_000, 0, "")

        assertFalse(budget.wouldExceed(today, today, budget.costUsd(AiCallKind.SCORE, estimate)))
        assertTrue(budget.wouldExceed(today, today, budget.costUsd(AiCallKind.ROAST, estimate)))
    }

    @Test
    fun `하루 사용량에 캐시 읽기 토큰을 따로 더해 비용에 반영한다`() {
        val day = AiUsageDay("2026-11-16")

        day.add(ModelUsage(5_000, 100, "STOP", 0, 1_000))

        assertEquals(1_000, day.cachedInputTokens)
        assertEquals(0, BigDecimal("0.0046").compareTo(day.costUsd(budget)))
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
    fun `생각 토큰은 출력 단가로 청구되므로 출력 토큰에 함께 더한다`() {
        val day = AiUsageDay("2026-11-16")

        day.add(ModelUsage(5_000, 100, "STOP", 40))

        assertEquals(140, day.outputTokens)
    }

    @Test
    fun `오늘 비용이 하루 한도에 닿으면 막는다`() {
        val atLimit = days(ModelUsage(500_000, 0, ""))
        val belowLimit = days(ModelUsage(499_999, 0, ""))

        assertTrue(budget.isExhausted(atLimit, atLimit))
        assertFalse(budget.isExhausted(belowLimit, belowLimit))
    }

    @Test
    fun `하루 한도가 없거나 0이면 하루 비용으로는 막지 않는다`() {
        val heavyDay = days(ModelUsage(5_000_000, 0, ""))

        listOf(null, BigDecimal.ZERO).forEach { daily ->
            val noDaily = AiBudget(daily, BigDecimal("9"), SCORE_PRICE, ROAST_PRICE)
            assertFalse(noDaily.isExhausted(heavyDay, heavyDay))
            assertFalse(noDaily.wouldExceed(heavyDay, heavyDay, scoreCost(ModelUsage(1_000_000, 0, ""))))
        }
    }

    @Test
    fun `호출 전 어림 비용을 더해 하루나 한 달 한도를 넘으면 넘는다고 본다`() {
        val today = days(ModelUsage(400_000, 0, ""))
        val month = days(ModelUsage(8_900_000, 0, ""))

        assertTrue(budget.wouldExceed(today, today, scoreCost(ModelUsage(100_001, 0, ""))))
        assertFalse(budget.wouldExceed(today, today, scoreCost(ModelUsage(100_000, 0, ""))))
        assertTrue(budget.wouldExceed(days(ModelUsage(0, 0, "")), month, scoreCost(ModelUsage(100_001, 0, ""))))
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

    private fun scoreCost(usage: ModelUsage): BigDecimal = budget.costUsd(AiCallKind.SCORE, usage)

    companion object {
        /** 채점: 입력 1달러, 캐시 읽기 0.1달러, 출력·생각 5달러(100만 토큰당). */
        private val SCORE_PRICE = AiModelPrice(BigDecimal("1"), BigDecimal("0.1"), BigDecimal("5"), BigDecimal("5"))

        /** 작가: 입력 2달러, 캐시 읽기 0.2달러, 출력 10달러, 생각 20달러(100만 토큰당). 생각 단가를 일부러 다르게 둬 따로 세는지 본다. */
        private val ROAST_PRICE = AiModelPrice(BigDecimal("2"), BigDecimal("0.2"), BigDecimal("10"), BigDecimal("20"))
    }
}
