package com.santacut.domain.analysis.model

import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScoringChunkTest {
    @Test
    fun `앞 구간과 겹친 줄은 근거 범위에서 빠지고 겹친 마지막 줄 번호를 알려 준다`() {
        val chunk = ScoringChunk(selfTurns(4), FIRST_NUMBER, OVERLAP, 0)

        assertEquals(7..8, chunk.lineRange())
        assertEquals(6, chunk.lastOverlapNumber())
        assertNull(ScoringChunk(selfTurns(4), FIRST_NUMBER, 0, 0).lastOverlapNumber())
    }

    @Test
    fun `모델이 겹친 줄을 근거로 고르면 그 번호는 지우고 한마디도 그 줄에서 고르지 않는다`() {
        val chunk = ScoringChunk(selfTurns(4), FIRST_NUMBER, OVERLAP, 0)
        val overlapOnly = ScoreAnswer.of(List(4) { BigDecimal(90) }, List(4) { listOf(5, 6) }, "test", opportunities = List(4) { 2 })
        val mixed = ScoreAnswer.of(List(4) { BigDecimal(90) }, List(4) { listOf(6, 7) }, "test", opportunities = List(4) { 2 })

        assertTrue(ScoringChunkAnswer(chunk, overlapOnly).readings().lowConfidence().coversAll())
        assertEquals(8, ScoringChunkAnswer(chunk, overlapOnly).discardedEvidence())
        assertEquals(4, ScoringChunkAnswer(chunk, mixed).discardedEvidence())
        assertEquals("고마운 말 2", ScoringChunkAnswer(chunk, mixed).quoteFor(ScoreDimension.CARE_EMPATHY, MAX_QUOTE_CHARS))
    }

    private fun selfTurns(count: Int): TranscriptTurns =
        TranscriptTurns(List(count) { index -> TranscriptTurn(ScoringMessage.SELF, NOON, NOON, "고마운 말 $index") })

    companion object {
        private const val FIRST_NUMBER = 5
        private const val OVERLAP = 2
        private const val MAX_QUOTE_CHARS = 20
        private val NOON: LocalDateTime = LocalDateTime.of(2026, 11, 1, 12, 0)
    }
}
