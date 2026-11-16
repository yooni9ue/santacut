package com.santacut.domain.analysis.model

import com.santacut.domain.global.config.ScoreShrinkageProperties
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
            readings.shrunkScores(ScoreShrinkage(BigDecimal.ZERO, 1, personalPrior = false), SELF_TURNS),
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

    @Test
    fun `수축 강도가 있어도 근거 없는 항목은 수축하지 않은 본인 수준 사전값이고 근거 있는 항목만 당긴다`() {
        val readings = readingsOf(listOf(90, 10, 70, 80), listOf(listOf(1), emptyList(), listOf(3), listOf(4)), listOf(2, 0, 1, 2))

        val shrunk = readings.shrunkScores(ScoreShrinkage(BigDecimal("0.25"), 5, personalPrior = true), SELF_TURNS)

        assertEquals(0, BigDecimal("73.75").compareTo(shrunk[1]))
        assertEquals(0, BigDecimal(67).compareTo(shrunk[2]))
    }

    @Test
    fun `여러 구간이면 구간을 합친 항목 평균으로 본인 수준 사전값을 낸다`() {
        val first = readingsOf(listOf(90, 10, 70, 80), listOf(listOf(1), emptyList(), emptyList(), emptyList()), listOf(2, 2, 2, 2))
        val second = readingsOf(listOf(60, 20, 50, 40), listOf(listOf(5), emptyList(), listOf(6), emptyList()), listOf(2, 2, 2, 2))

        val shrunk = (first + second).shrunkScores(ScoreShrinkage(BigDecimal.ZERO, 1, personalPrior = true), SELF_TURNS)

        assertEquals(listOf(75, 60, 50, 60), shrunk.map { score -> score.toInt() })
        assertEquals(0, BigDecimal(60).compareTo(shrunk[3]))
    }

    @Test
    fun `근거 있는 항목이 하나뿐이면 사전값은 중립 점수와 그 항목 평균의 가운데다`() {
        val readings = readingsOf(listOf(90, 10, 20, 30), listOf(listOf(1), emptyList(), emptyList(), emptyList()), listOf(2, 2, 2, 2))

        val shrunk = readings.shrunkScores(ScoreShrinkage(BigDecimal.ZERO, 1, personalPrior = true), SELF_TURNS)

        assertEquals(BigDecimal(90), shrunk[0])
        assertTrue(shrunk.drop(1).all { score -> BigDecimal("72.5").compareTo(score) == 0 })
    }

    @Test
    fun `수축 설정은 강도, 본인 턴 수, 본인 수준 사전값 여부를 그대로 넘기고 기본은 사전값을 쓴다`() {
        assertEquals(ScoreShrinkage(BigDecimal("0.25"), 5, personalPrior = true), ScoreShrinkageProperties().toShrinkage())
        assertEquals(
            ScoreShrinkage(BigDecimal.ONE, 20, personalPrior = false),
            ScoreShrinkageProperties(BigDecimal.ONE, 20, personalPrior = false).toShrinkage(),
        )
    }

    private fun readingsOf(scores: List<Int>, evidence: List<List<Int>>, opportunities: List<Int?>): DimensionReadings =
        DimensionScores(scores.map(::BigDecimal)).toReadings(EvidenceIndices(evidence), DimensionOpportunities(opportunities), SELF_TURNS)

    companion object {
        private const val SELF_TURNS = 20
    }
}
