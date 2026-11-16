package com.santacut.domain.analysis.model

import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ScoreAnswersTest {
    @Test
    fun `답이 하나면 그 답을 그대로 돌려준다`() {
        val answer = scored(listOf(70, 60, 50, 40), opportunity = 1, playfulness = 30, evidence = 1)

        assertSame(answer, ScoreAnswers(listOf(answer)).median())
    }

    @Test
    fun `점수, 기회 수, 장난기는 항목마다 중앙값이다`() {
        val median = ScoreAnswers(
            listOf(
                scored(listOf(80, 60, 50, 40), opportunity = 2, playfulness = 70, evidence = 1),
                scored(listOf(72, 66, 54, 41), opportunity = 0, playfulness = 20, evidence = 2),
                scored(listOf(75, 62, 58, 49), opportunity = 1, playfulness = 40, evidence = 3),
            ),
        ).median()

        assertEquals(listOf(75, 62, 54, 41).map { BigDecimal(it) }, ScoreDimension.entries.indices.map { median.scoreAt(it) })
        assertEquals(1, median.opportunityAt(0))
        assertEquals(40, median.playfulness)
    }

    @Test
    fun `근거 줄은 중앙값에 가장 가까운 답의 것이다`() {
        val median = ScoreAnswers(
            listOf(
                scored(listOf(80, 80, 80, 80), evidence = 1),
                scored(listOf(60, 60, 60, 60), evidence = 2),
                scored(listOf(70, 70, 70, 71), evidence = 3),
            ),
        ).median()

        assertEquals(listOf(3), median.evidenceAt(0))
    }

    @Test
    fun `사용량은 모든 호출의 합이다`() {
        val median = ScoreAnswers(List(3) { scored(listOf(70, 60, 50, 40), evidence = 1) }).median()

        assertEquals(30, median.usage.inputTokens)
    }

    @Test
    fun `한 호출이라도 제공자가 막으면 작업 전체가 그 이유로 끝난다`() {
        val median = ScoreAnswers(listOf(scored(listOf(70, 60, 50, 40), evidence = 1), ScoreAnswer.providerUnavailable())).median()

        val exception = assertThrows<SantaCutException> { median.verifyAnswered() }
        assertEquals(ErrorCode.AI_PROVIDER_UNAVAILABLE, exception.errorCode)
    }

    @Test
    fun `근거 부족 답이 절반을 넘으면 근거 부족이고, 절반 미만이면 점수 답만으로 중앙값을 낸다`() {
        val insufficient = ScoreAnswer.insufficient(USAGE)
        val mostlyInsufficient = ScoreAnswers(listOf(scored(listOf(70, 60, 50, 40), evidence = 1), insufficient, insufficient)).median()
        val mostlyScored = ScoreAnswers(
            listOf(scored(listOf(70, 60, 50, 40), evidence = 1), scored(listOf(71, 61, 51, 41), evidence = 1), insufficient),
        ).median()

        assertFalse(mostlyInsufficient.isScored())
        assertTrue(mostlyScored.isValidScore())
        assertEquals(BigDecimal("70.50"), mostlyScored.scoreAt(0))
    }

    @Test
    fun `여러 호출은 동시에 불리고 모두 모인다`() {
        val answers = ScoreAnswers.sampled(3) { scored(listOf(70, 60, 50, 40), evidence = 1) }.median()

        assertEquals(30, answers.usage.inputTokens)
    }

    private fun scored(scores: List<Int>, opportunity: Int = 2, playfulness: Int? = null, evidence: Int): ScoreAnswer =
        ScoreAnswer.of(scores.map { BigDecimal(it) }, scores.map { listOf(evidence) }, "test", USAGE, scores.map { opportunity }, playfulness)

    companion object {
        private val USAGE = ModelUsage(10, 5, "STOP")
    }
}
