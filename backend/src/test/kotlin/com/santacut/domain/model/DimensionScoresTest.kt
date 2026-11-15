package com.santacut.domain.model

import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DimensionScoresTest {
    @Test
    fun `모든 항목이 숫자로 들어와야 완전하다`() {
        assertTrue(DimensionScores(listOf(BigDecimal(1), BigDecimal(2), BigDecimal(3), BigDecimal(4))).isComplete())
        assertFalse(DimensionScores(listOf(BigDecimal(1), null, BigDecimal(3), BigDecimal(4))).isComplete())
        assertFalse(DimensionScores(listOf(BigDecimal(1), BigDecimal(2), BigDecimal(3))).isComplete())
    }

    @Test
    fun `숫자인 항목 점수만 순서대로 남긴다`() {
        val scores = DimensionScores(listOf(BigDecimal(1), null, BigDecimal(3)))
        assertEquals(listOf(BigDecimal(1), BigDecimal(3)), scores.toComponents())
    }
}
