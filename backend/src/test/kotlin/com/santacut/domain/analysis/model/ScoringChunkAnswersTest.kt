package com.santacut.domain.analysis.model

import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ScoringChunkAnswersTest {
    @Test
    fun `구간 점수를 본인 턴 수와 기회 수로 가중 평균한다`() {
        val first = chunkAnswer(chunk(1, 20), scored(90, opportunity = 2))
        val second = chunkAnswer(chunk(21, 20), scored(30, opportunity = 0, firstNumber = 21))

        val score = ScoringChunkAnswers(listOf(first, second)).toScore(ScoreShrinkage.NONE)

        assertEquals(BigDecimal(75), score.components.scoreOf(ScoreDimension.CARE_EMPATHY))
    }

    @Test
    fun `근거 부족 구간은 빼고 합치며 모두 근거 부족이면 근거 부족 오류다`() {
        val answers = ScoringChunkAnswers(listOf(chunkAnswer(chunk(1, 20), ScoreAnswer.insufficient()), chunkAnswer(chunk(21, 20), scored(70, firstNumber = 21))))

        assertEquals(7000, answers.toScore(ScoreShrinkage.NONE).hundredths)
        val none = ScoringChunkAnswers(listOf(chunkAnswer(chunk(1, 20), ScoreAnswer.insufficient())))
        assertEquals(ErrorCode.INSUFFICIENT_EVIDENCE, assertFailsWith<SantaCutException> { none.toScore(ScoreShrinkage.NONE) }.errorCode)
    }

    @Test
    fun `한 구간이라도 읽을 수 없으면 작업 전체가 응답 오류다`() {
        val answers = ScoringChunkAnswers(listOf(chunkAnswer(chunk(1, 20), scored(70)), chunkAnswer(chunk(21, 20), ScoreAnswer.unreadable())))

        assertEquals(ErrorCode.AI_RESPONSE_INVALID, assertFailsWith<SantaCutException> { answers.toScore(ScoreShrinkage.NONE) }.errorCode)
    }

    @Test
    fun `수축한 정밀 점수로 합계를 한 번만 반올림한다`() {
        val shrinkage = ScoreShrinkage(BigDecimal(55), BigDecimal.ONE, 20)

        val score = ScoringChunkAnswers(listOf(chunkAnswer(chunk(1, 40), scored(80)))).toScore(shrinkage)

        assertEquals(7167, score.hundredths)
        assertEquals(BigDecimal("71.67"), score.components.scoreOf(ScoreDimension.CARE_EMPATHY))
    }

    @Test
    fun `장난기는 구간 본인 턴 수로 평균한다`() {
        val answers = ScoringChunkAnswers(
            listOf(chunkAnswer(chunk(1, 20), scored(70, playfulness = 90)), chunkAnswer(chunk(21, 60), scored(70, playfulness = 10, firstNumber = 21))),
        )

        assertEquals(30, answers.toScore(ScoreShrinkage.NONE).playfulness)
    }

    @Test
    fun `한마디는 본인 근거 줄에서 20자 이하 토막만 고르고 상대 줄은 고르지 않는다`() {
        val turns = TranscriptTurns(
            listOf(turn(PARTNER, "힘내 고마워 진짜 덕분이야"), turn(ScoringMessage.SELF, "아주 길게 이어지는 본인 말이라서 스무 자를 넘는다 / 덕분에 살았다")),
        )
        val chunk = ScoringChunk(turns, 1, 0, 0)
        val answer = ScoreAnswer.of(List(4) { BigDecimal(70) }, listOf(listOf(1, 2), listOf(1), listOf(1), listOf(1)), "test")
        val answers = ScoringChunkAnswers(listOf(chunkAnswer(chunk, answer)))
        val partnerOnly = ScoringChunkAnswers(
            listOf(chunkAnswer(chunk, ScoreAnswer.of(List(4) { BigDecimal(70) }, List(4) { listOf(1) }, "test"))),
        )

        assertEquals("덕분에 살았다", answers.quoteOf(answers.toScore(ScoreShrinkage.NONE)))
        assertNull(partnerOnly.quoteOf(partnerOnly.toScore(ScoreShrinkage.NONE)))
    }

    private fun scored(value: Int, opportunity: Int = 2, playfulness: Int? = null, firstNumber: Int = 1): ScoreAnswer =
        ScoreAnswer.of(
            List(4) { BigDecimal(value) }, List(4) { listOf(firstNumber + it) }, "test", ModelUsage.NONE, List(4) { opportunity }, playfulness,
        )

    private fun chunkAnswer(chunk: ScoringChunk, answer: ScoreAnswer): ScoringChunkAnswer = ScoringChunkAnswer(chunk, answer)

    /** [firstNumber]부터 [count]줄, 본인과 상대가 번갈아 말한 구간. */
    private fun chunk(firstNumber: Int, count: Int): ScoringChunk = ScoringChunk(
        TranscriptTurns(List(count) { index -> turn(if (index % 2 == 0) ScoringMessage.SELF else PARTNER, "말 $index") }),
        firstNumber, 0, 0,
    )

    private fun turn(speaker: String, text: String): TranscriptTurn = TranscriptTurn(speaker, NOON, NOON, text)

    companion object {
        private const val PARTNER = "상대"
        private val NOON: LocalDateTime = LocalDateTime.of(2026, 11, 1, 12, 0)

    }
}
