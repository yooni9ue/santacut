package com.santacut.domain.analysis.model

import com.santacut.ErrorCodeAssertions.assertErrorCode
import com.santacut.domain.analysis.model.ScoringDigest.Companion.MIN_SAMPLE_CHARS
import com.santacut.domain.analysis.model.ScoringDigest.Companion.MIN_SAMPLE_LINES
import com.santacut.domain.global.exception.ErrorCode
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals

/**
 * 호출 전 차단 문턱(표본 8줄, 본문 300자)을 확인한다.
 */
class ScoringDigestTest {
    private val noon = LocalDateTime.of(2026, 11, 14, 12, 0)

    @Test
    fun `표본이 8줄 이상이고 본문이 300자 이상이면 표본 크기가 근거 번호 상한이다`() {
        val digest = digestOf(List(MIN_SAMPLE_LINES) { line(it, MIN_SAMPLE_CHARS / MIN_SAMPLE_LINES + 1) } + message("ㅋㅋㅋ"))

        assertEquals(MIN_SAMPLE_LINES, digest.verifySampled().sampleSize())
        assertEquals(MIN_SAMPLE_LINES + 1, digest.features.eventCount)
    }

    @Test
    fun `표본이 8줄보다 적으면 AI를 부르지 않고 INSUFFICIENT_EVIDENCE다`() {
        val digest = digestOf(List(MIN_SAMPLE_LINES - 1) { line(it, MIN_SAMPLE_CHARS) })

        assertErrorCode(ErrorCode.INSUFFICIENT_EVIDENCE) { digest.verifySampled() }
    }

    @Test
    fun `표본 본문이 300자보다 짧으면 AI를 부르지 않고 INSUFFICIENT_EVIDENCE다`() {
        val digest = digestOf(List(MIN_SAMPLE_LINES * 2) { line(it, MIN_SAMPLE_CHARS / (MIN_SAMPLE_LINES * 2) - 1) })

        assertErrorCode(ErrorCode.INSUFFICIENT_EVIDENCE) { digest.verifySampled() }
    }

    /** [number]를 앞에 붙여 [length]자로 맞춘 메시지. */
    private fun line(number: Int, length: Int): ScoringMessage = message("$number".padEnd(length, '가'))

    private fun message(text: String): ScoringMessage = ScoringMessage(noon, text)

    private fun digestOf(messages: List<ScoringMessage>): ScoringDigest {
        val scoringMessages = ScoringMessages(messages)
        val sample = scoringMessages.sample()
        return ScoringDigest(DataSource.KAKAO, scoringMessages.toFeatures(), scoringMessages.signals(sample), sample)
    }
}
