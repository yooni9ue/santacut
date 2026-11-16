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
        assertFalse(DimensionScores(listOf(BigDecimal("10.005"), BigDecimal(10), BigDecimal(10), BigDecimal(10))).isWithinRange())
    }

    @Test
    fun `항목마다 점수, 기회 수, 근거 번호를 묶는다`() {
        val scores = DimensionScores(listOf(BigDecimal(90), BigDecimal(10), BigDecimal(70), BigDecimal(30)))
        val evidence = EvidenceIndices(listOf(listOf(1), emptyList(), listOf(3), listOf(4)))

        val readings = scores.toReadings(evidence, DimensionOpportunities(listOf(2, 0, 1, null)), SELF_TURNS)

        assertEquals(listOf("positivity_support"), readings.lowConfidence().toKeys())
        assertEquals(
            listOf(BigDecimal(90), DimensionScores.NEUTRAL_SCORE, BigDecimal(70), BigDecimal(30)),
            readings.shrunkScores(ScoreShrinkage(BigDecimal.ZERO, 1), SELF_TURNS),
        )
    }

    @Test
    fun `본인 수준 사전값을 쓰면 근거 없는 항목은 중립 점수와 다른 항목 평균을 함께 평균한 값이다`() {
        val scores = DimensionScores(listOf(BigDecimal(90), BigDecimal(10), BigDecimal(70), BigDecimal(80)))
        val evidence = EvidenceIndices(listOf(listOf(1), emptyList(), listOf(3), listOf(4)))
        val readings = scores.toReadings(evidence, DimensionOpportunities(listOf(2, 0, 1, 2)), SELF_TURNS)

        val shrunk = readings.shrunkScores(ScoreShrinkage(BigDecimal.ZERO, 1, personalPrior = true), SELF_TURNS)

        assertEquals(0, BigDecimal("73.75").compareTo(shrunk[1]))
        assertEquals(BigDecimal(90), shrunk[0])
    }

    companion object {
        private const val SELF_TURNS = 20
    }
}
