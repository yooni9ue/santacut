package com.santacut.domain.model

import com.santacut.domain.exception.ErrorCode
import com.santacut.domain.exception.SantaCutException
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ScoreAnswerTest {
    private val fullScores: List<BigDecimal?> = listOf(BigDecimal(80), BigDecimal(90), BigDecimal(70), BigDecimal(60))
    private val validEvidence = listOf(listOf(1), listOf(2), listOf(3, 4), listOf(20))

    @Test
    fun `근거 번호가 모두 이벤트 범위 안이면 가중 점수가 된다`() {
        assertEquals(7600, ScoreAnswer.of(fullScores, validEvidence, SHAPE).toScore(EVENT_COUNT).hundredths)
    }

    @Test
    fun `읽을 수 없는 응답은 AI 응답 오류다`() {
        assertRejected(ErrorCode.AI_RESPONSE_INVALID, ScoreAnswer.unreadable())
    }

    @Test
    fun `모델이 근거 부족이라고 답하면 근거 부족 오류다`() {
        assertRejected(ErrorCode.INSUFFICIENT_EVIDENCE, ScoreAnswer.insufficient())
    }

    @Test
    fun `항목 점수가 빠졌거나 숫자가 아니면 형식 오류다`() {
        assertRejected(ErrorCode.AI_RESPONSE_SCHEMA_INVALID, ScoreAnswer.of(fullScores.take(3), validEvidence, SHAPE))
        val withText = listOf(BigDecimal(80), null, BigDecimal(70), BigDecimal(60))
        assertRejected(ErrorCode.AI_RESPONSE_SCHEMA_INVALID, ScoreAnswer.of(withText, validEvidence, SHAPE))
    }

    @Test
    fun `근거가 없는 항목이 있으면 형식 오류다`() {
        val missingEvidence = listOf(listOf(1), emptyList(), listOf(3), listOf(4))
        assertRejected(ErrorCode.AI_RESPONSE_SCHEMA_INVALID, ScoreAnswer.of(fullScores, missingEvidence, SHAPE))
    }

    @Test
    fun `근거 번호가 1보다 작거나 이벤트 수보다 크면 근거 오류다`() {
        val belowRange = listOf(listOf(0), listOf(1), listOf(1), listOf(1))
        val aboveRange = listOf(listOf(1), listOf(21), listOf(1), listOf(1))
        assertRejected(ErrorCode.AI_EVIDENCE_INVALID, ScoreAnswer.of(fullScores, belowRange, SHAPE))
        assertRejected(ErrorCode.AI_EVIDENCE_INVALID, ScoreAnswer.of(fullScores, aboveRange, SHAPE))
    }

    private fun assertRejected(expected: ErrorCode, answer: ScoreAnswer) {
        val exception = assertFailsWith<SantaCutException> { answer.toScore(EVENT_COUNT) }
        assertEquals(expected, exception.errorCode)
    }

    companion object {
        private const val EVENT_COUNT = 20
        private const val SHAPE = "scores=OBJECT,evidence=OBJECT"
    }
}
