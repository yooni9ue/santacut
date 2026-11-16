package com.santacut.domain.analysis.model

import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScoringChunksTest {
    @Test
    fun `짧은 대화는 한 구간이다`() {
        var calls = 0
        ScoringChunks.of(digestOf(turns(10, 100))).ask { calls++; ScoreAnswer.insufficient() }

        assertEquals(1, calls)
    }

    @Test
    fun `본문이 길면 최대 4구간으로 나누고 앞 구간 끝 줄을 겹쳐 싣되 겹친 줄은 근거 범위에서 뺀다`() {
        val asked = mutableListOf<ScoringChunk>()
        ScoringChunks.of(digestOf(turns(1000, 50))).ask { chunk -> asked += chunk; ScoreAnswer.insufficient() }

        assertEquals(ScoringChunks.MAX_CHUNKS, asked.size)
        assertEquals(1..250, asked[0].lineRange())
        assertEquals(251..500, asked[1].lineRange())
        assertEquals(247, asked[1].firstNumber)
        assertEquals(125, asked[1].selfTurnCount())
    }

    @Test
    fun `한 구간이라도 제공자가 막으면 남은 구간은 묻지 않는다`() {
        var calls = 0
        val answers = ScoringChunks.of(digestOf(turns(1000, 50))).ask { calls++; ScoreAnswer.providerUnavailable() }

        assertEquals(1, calls)
        assertEquals(1, answers.size())
    }

    @Test
    fun `어림 사용량은 모든 구간의 합이다`() {
        val estimate = ScoringChunks.of(digestOf(turns(1000, 50))).estimate { ModelUsage(100, 10, "ESTIMATE") }

        assertEquals(400, estimate.inputTokens)
        assertEquals(40, estimate.outputTokens)
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
        private const val PARTNER = "상대"
        private val NOON: LocalDateTime = LocalDateTime.of(2026, 11, 1, 12, 0)
    }
}
