package com.santacut.domain.analysis.model

import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime
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
    fun `근거 목록이 빠진 항목이 있으면 형식 오류다`() {
        val missingEvidence = listOf(listOf(1), listOf(3), listOf(4))
        assertRejected(ErrorCode.AI_RESPONSE_SCHEMA_INVALID, ScoreAnswer.of(fullScores, missingEvidence, SHAPE))
    }

    @Test
    fun `근거가 빈 항목은 실패시키지 않고 중립 점수를 주고 근거 부족 항목으로 표시한다`() {
        val score = ScoreAnswer.of(fullScores, listOf(listOf(1), emptyList(), listOf(3), listOf(4)), SHAPE).toScore(EVENT_COUNT)

        assertEquals(listOf(BigDecimal(80), DimensionScores.NEUTRAL_SCORE, BigDecimal(70), BigDecimal(60)), score.components.toList())
        assertEquals(listOf("positivity_support"), score.lowConfidence.toKeys())
        assertEquals(6725, score.hundredths)
    }

    @Test
    fun `범위 밖 근거 번호만 있는 항목도 근거 부족 항목이다`() {
        val aboveRange = listOf(listOf(1), listOf(21, 22), listOf(0), listOf(1))
        val score = ScoreAnswer.of(fullScores, aboveRange, SHAPE).toScore(EVENT_COUNT)

        assertEquals(listOf("positivity_support", "respectful_communication"), score.lowConfidence.toKeys())
    }

    @Test
    fun `모든 항목에 근거가 없으면 처음부터 비었을 때 근거 부족, 번호를 지워서 비었을 때 근거 오류다`() {
        val empty = List(ScoreDimension.entries.size) { emptyList<Int>() }
        val outOfRange = List(ScoreDimension.entries.size) { listOf(EVENT_COUNT + 1) }

        assertRejected(ErrorCode.INSUFFICIENT_EVIDENCE, ScoreAnswer.of(fullScores, empty, SHAPE))
        assertRejected(ErrorCode.AI_EVIDENCE_INVALID, ScoreAnswer.of(fullScores, outOfRange, SHAPE))
    }

    @Test
    fun `범위 밖·겹친 번호를 지우고도 항목마다 근거가 남으면 다시 묻지 않고 점수가 된다`() {
        val answer = ScoreAnswer.of(fullScores, listOf(listOf(1, 21), listOf(2, 2), listOf(0, 3), listOf(20)), SHAPE)

        assertEquals(7600, answer.toScore(EVENT_COUNT).hundredths)
        assertEquals(3, answer.discardedEvidence(1..EVENT_COUNT))
    }

    @Test
    fun `제공자가 호출을 막았으면 AI_PROVIDER_UNAVAILABLE이고 사용량은 없다`() {
        val answer = ScoreAnswer.providerUnavailable()

        assertRejected(ErrorCode.AI_PROVIDER_UNAVAILABLE, answer)
        assertEquals(ModelUsage.NONE, answer.usage)
    }

    @Test
    fun `답은 받은 토큰 사용량을 그대로 들고 있다`() {
        val usage = ModelUsage(4200, 90, "end_turn")

        assertEquals(usage, ScoreAnswer.of(fullScores, validEvidence, SHAPE, usage).usage)
        assertEquals(usage, ScoreAnswer.unreadable(usage).usage)
    }

    /** 본인 줄 [lines]개짜리 한 구간의 답으로 보고 수축 없이 점수를 낸다. */
    private fun ScoreAnswer.toScore(lines: Int): Score {
        val turns = TranscriptTurns(List(lines) { index -> TranscriptTurn(ScoringMessage.SELF, START, START, "말 $index") })
        return ScoringChunkAnswers(listOf(ScoringChunkAnswer(ScoringChunk(turns, 1, 0, 0), this))).toScore(ScoreShrinkage(BigDecimal.ZERO, 1, personalPrior = false))
    }

    private fun assertRejected(expected: ErrorCode, answer: ScoreAnswer) {
        val exception = assertFailsWith<SantaCutException> { answer.toScore(EVENT_COUNT) }
        assertEquals(expected, exception.errorCode)
    }

    companion object {
        private const val EVENT_COUNT = 20
        private val START: LocalDateTime = LocalDateTime.of(2026, 11, 1, 12, 0)
        private const val SHAPE = "scores=OBJECT,evidence=OBJECT"
    }
}
