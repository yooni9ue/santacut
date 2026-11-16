package com.santacut.domain.analysis.model

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
    fun `점수가 0~100이고 소수 둘째 자리까지면 범위 안이다`() {
        assertTrue(DimensionScores(listOf(BigDecimal(0), BigDecimal(100), BigDecimal("70.25"), BigDecimal(10))).isWithinRange())
        assertFalse(DimensionScores(listOf(BigDecimal(101), BigDecimal(10), BigDecimal(10), BigDecimal(10))).isWithinRange())
    }

    @Test
    fun `항목마다 점수, 기회 수, 근거 번호를 묶는다`() {
        val scores = DimensionScores(listOf(BigDecimal(90), BigDecimal(10), BigDecimal(70), BigDecimal(30)))
        val evidence = EvidenceIndices(listOf(listOf(1), emptyList(), listOf(3), listOf(4)))

        val readings = scores.toReadings(evidence, DimensionOpportunities(listOf(2, 0, 1, null)), SELF_TURNS)

        assertEquals(listOf("positivity_support"), readings.lowConfidence().toKeys())
        assertEquals(
            listOf(BigDecimal(90), DimensionScores.NEUTRAL_SCORE, BigDecimal(70), BigDecimal(30)),
            readings.shrunkScores(ScoreShrinkage.NONE, SELF_TURNS),
        )
    }

    companion object {
        private const val SELF_TURNS = 20
    }
}
