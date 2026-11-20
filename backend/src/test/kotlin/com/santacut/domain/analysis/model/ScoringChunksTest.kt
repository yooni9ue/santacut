package com.santacut.domain.analysis.model

import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScoringChunksTest {
    @Test
    fun `짧은 대화는 한 구간이다`() {
        var calls = 0
        ScoringChunks.of(digestOf(turns(10, 100))).ask(EACH_IN_TURN) { calls++; ScoreAnswer.insufficient() }

        assertEquals(1, calls)
    }

    @Test
    fun `본문이 길면 최대 8구간으로 나누고 앞 구간 끝 줄을 겹쳐 싣되 겹친 줄은 근거 범위에서 뺀다`() {
        val asked = mutableListOf<ScoringChunk>()
        ScoringChunks.of(digestOf(turns(1000, 50))).ask(EACH_IN_TURN) { chunk -> asked += chunk; ScoreAnswer.insufficient() }

        assertEquals(ScoringChunks.MAX_CHUNKS, asked.size)
        assertEquals(1..125, asked[0].lineRange())
        assertEquals(126..250, asked[1].lineRange())
        assertEquals(122, asked[1].firstNumber)
        assertEquals(62, asked[1].selfTurnCount())
    }

    @Test
    fun `모든 구간을 묻고 답을 구간 순서대로 실어 앞 구간의 쓸 수 없는 답으로 작업 전체가 끝난다`() {
        val calls = AtomicInteger()
        val reversed: (List<ScoringChunk>, (ScoringChunk) -> ScoreAnswer) -> List<ScoreAnswer> =
            { chunks, ask -> chunks.reversed().map(ask).reversed() }
        val answers = ScoringChunks.of(digestOf(turns(1000, 50))).ask(reversed) { chunk ->
            calls.incrementAndGet()
            if (chunk.firstNumber == 1) ScoreAnswer.providerUnavailable() else ScoreAnswer.unreadable(ModelUsage(100, 10, "STOP"))
        }

        assertEquals(ScoringChunks.MAX_CHUNKS, calls.get())
        assertEquals(ScoringChunks.MAX_CHUNKS, answers.size())
        assertEquals(700, answers.usage().inputTokens)
        val exception = assertThrows<SantaCutException> { answers.toScore(ScoreShrinkage(BigDecimal.ZERO, 1, personalPrior = false)) }
        assertEquals(ErrorCode.AI_PROVIDER_UNAVAILABLE, exception.errorCode)
    }

    @Test
    fun `분배기가 앞 구간 실패로 뒤 구간을 부르지 않고 답을 덜 돌려주면 돌려준 답까지만 싣고 그 실패로 끝난다`() {
        val firstOnly: (List<ScoringChunk>, (ScoringChunk) -> ScoreAnswer) -> List<ScoreAnswer> = { chunks, ask -> listOf(ask(chunks.first())) }

        val answers = ScoringChunks.of(digestOf(turns(1000, 50))).ask(firstOnly) { ScoreAnswer.providerUnavailable() }

        assertEquals(1, answers.size())
        val exception = assertThrows<SantaCutException> { answers.toScore(ScoreShrinkage(BigDecimal.ZERO, 1, personalPrior = false)) }
        assertEquals(ErrorCode.AI_PROVIDER_UNAVAILABLE, exception.errorCode)
    }

    @Test
    fun `어림 사용량은 모든 구간의 합이다`() {
        val estimate = ScoringChunks.of(digestOf(turns(1000, 50))).estimate(3) { ModelUsage(100, 10, "ESTIMATE") }

        assertEquals(800, estimate.inputTokens)
        assertEquals(80, estimate.outputTokens)
    }

    @Test
    fun `한 구간 대화의 어림 사용량은 반복 호출 수만큼 곱한다`() {
        val estimate = ScoringChunks.of(digestOf(turns(10, 100))).estimate(3) { ModelUsage(100, 10, "ESTIMATE") }

        assertEquals(300, estimate.inputTokens)
        assertEquals(30, estimate.outputTokens)
    }

    @Test
    fun `한 구간 대화는 반복 호출 수만큼 묻고 사용량을 모두 싣는다`() {
        val calls = AtomicInteger()
        val answers = ScoringChunks.of(digestOf(turns(10, 100))).ask(3, IN_TURN, EACH_IN_TURN) { calls.incrementAndGet(); ScoreAnswer.insufficient(ModelUsage(100, 10, "STOP")) }

        assertEquals(3, calls.get())
        assertEquals(1, answers.size())
        assertEquals(300, answers.usage().inputTokens)
    }

    @Test
    fun `반복 호출 수가 1이면 예전처럼 한 번만 묻고 그 답을 그대로 쓴다`() {
        var calls = 0
        val answer = ScoreAnswer.insufficient(ModelUsage(100, 10, "STOP"))
        val answers = ScoringChunks.of(digestOf(turns(10, 100))).ask(1, IN_TURN, EACH_IN_TURN) { calls++; answer }

        assertEquals(1, calls)
        assertEquals(answer.usage, answers.usage())
    }

    @Test
    fun `여러 구간 대화는 반복 호출 수와 상관없이 구간마다 한 번 묻는다`() {
        val calls = AtomicInteger()
        ScoringChunks.of(digestOf(turns(1000, 50))).ask(3, IN_TURN, EACH_IN_TURN) { calls.incrementAndGet(); ScoreAnswer.insufficient() }

        assertEquals(ScoringChunks.MAX_CHUNKS, calls.get())
    }

    @Test
    fun `경계는 본문 길이가 비슷하게 나뉜다`() {
        val bounds = TranscriptTurns(turns(8, 10)).partitionBounds(4)

        assertEquals(listOf(0, 2, 4, 6, 8), bounds)
        assertTrue(TranscriptTurns(turns(2, 10)).partitionBounds(4).size <= 3)
    }

    private fun digestOf(turns: List<TranscriptTurn>): ScoringDigest = ScoringDigest(turns.size, turns.size / 2, TranscriptTurns(turns), 0)

    /** 본인과 상대가 번갈아 말한 [count]줄, 줄마다 [length]자. */
    private fun turns(count: Int, length: Int): List<TranscriptTurn> = List(count) { index ->
        TranscriptTurn(if (index % 2 == 0) ScoringMessage.SELF else PARTNER, NOON, NOON, "가".repeat(length))
    }

    companion object {
        /** 반복 호출을 차례로 부르는 표본기. */
        private val IN_TURN: (Int, () -> ScoreAnswer) -> List<ScoreAnswer> = { count, asker -> List(count) { asker() } }

        /** 구간을 차례로 부르는 분배기. */
        private val EACH_IN_TURN: (List<ScoringChunk>, (ScoringChunk) -> ScoreAnswer) -> List<ScoreAnswer> = { chunks, ask -> chunks.map(ask) }

        private const val PARTNER = "상대"
        private val NOON: LocalDateTime = LocalDateTime.of(2026, 11, 1, 12, 0)
    }
}
