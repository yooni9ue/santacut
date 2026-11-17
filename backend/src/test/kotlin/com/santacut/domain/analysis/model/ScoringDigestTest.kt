package com.santacut.domain.analysis.model

import com.santacut.ErrorCodeAssertions.assertErrorCode
import com.santacut.domain.analysis.model.ScoringDigest.Companion.MAX_TRANSCRIPT_CHARS
import com.santacut.domain.analysis.model.ScoringDigest.Companion.MIN_SELF_CHARS
import com.santacut.domain.analysis.model.ScoringDigest.Companion.MIN_SELF_MESSAGES
import com.santacut.domain.global.exception.ErrorCode
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 호출 전 차단 문턱(본인 메시지 8개, 본인 본문 300자)과 대화문 길이 한도를 확인한다.
 */
class ScoringDigestTest {
    private val start = LocalDateTime.of(2026, 11, 1, 9, 0)

    @Test
    fun `본인 메시지가 8개 이상이고 본인 본문이 300자 이상이면 대화문 줄 수가 근거 번호 상한이다`() {
        val digest = digestOf(exchanges(MIN_SELF_MESSAGES, MIN_SELF_CHARS / MIN_SELF_MESSAGES + 1))

        assertEquals(MIN_SELF_MESSAGES * 2, digest.verifyEnough().lineCount())
        assertEquals(0, digest.omittedTurns)
    }

    @Test
    fun `본인 메시지가 8개보다 적으면 AI를 부르지 않고 INSUFFICIENT_EVIDENCE다`() {
        val digest = digestOf(exchanges(MIN_SELF_MESSAGES - 1, MIN_SELF_CHARS))

        assertErrorCode(ErrorCode.INSUFFICIENT_EVIDENCE) { digest.verifyEnough() }
    }

    @Test
    fun `본인 본문이 300자보다 짧으면 상대 말이 길어도 AI를 부르지 않고 INSUFFICIENT_EVIDENCE다`() {
        val digest = digestOf(exchanges(MIN_SELF_MESSAGES * 2, MIN_SELF_CHARS / (MIN_SELF_MESSAGES * 2) - 1))

        assertErrorCode(ErrorCode.INSUFFICIENT_EVIDENCE) { digest.verifyEnough() }
    }

    @Test
    fun `대화문이 길이 한도를 넘으면 최근 줄만 남기고 뺀 줄 수를 적는다`() {
        val length = 250
        val digest = digestOf(exchanges(MAX_TRANSCRIPT_CHARS / length, length))

        assertEquals(MAX_TRANSCRIPT_CHARS / length, digest.lineCount())
        assertEquals(MAX_TRANSCRIPT_CHARS / length, digest.omittedTurns)
    }

    /** 상대 한 줄과 본인 한 줄([length]자)을 [count]번 주고받은 대화. */
    @Test
    fun `혼잣말 수백 개를 잇달아 보내도 한 줄로 합치지 않고 나눠 최근 줄을 남긴다`() {
        val monologue = List(MONOLOGUE_MESSAGES) { index -> ScoringMessage(start.plusMinutes(index.toLong()), "오늘 있었던 일을 길게 적어 본다 ".repeat(14) + index) }

        val digest = digestOf(monologue).verifyEnough()

        assertTrue(digest.lineCount() > 1)
        assertTrue(digest.turns.selfLength() in MIN_SELF_CHARS..MAX_TRANSCRIPT_CHARS)
    }

    private fun exchanges(count: Int, length: Int): List<ScoringMessage> = (0 until count).flatMap { index ->
        val time = start.plusMinutes(index * 2L)
        listOf(ScoringMessage(time, "가".repeat(length), "상대"), ScoringMessage(time.plusMinutes(1), "$index".padEnd(length, '나')))
    }

    private fun digestOf(messages: List<ScoringMessage>): ScoringDigest = ScoringDigest.of(ScoringMessages(messages))

    companion object {
        private const val MONOLOGUE_MESSAGES = 200
    }
}
