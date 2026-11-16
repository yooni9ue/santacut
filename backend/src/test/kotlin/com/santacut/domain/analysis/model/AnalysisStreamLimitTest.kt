package com.santacut.domain.analysis.model

import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals

class AnalysisStreamLimitTest {
    private val limit = AnalysisStreamLimit(MAX_OPEN, RETRY_DELAY)
    private val now = Instant.parse("2026-11-16T03:00:00Z")

    @Test
    fun `상한보다 적게 열려 있으면 연다`() {
        limit.verifyOpenable(MAX_OPEN - 1, false, now)
    }

    @Test
    fun `상한에 닿으면 ANALYSIS_STREAM_LIMIT이고 다시 열어 볼 시각을 알린다`() {
        val exception = assertThrows<SantaCutException> { limit.verifyOpenable(MAX_OPEN, false, now) }

        assertEquals(ErrorCode.ANALYSIS_STREAM_LIMIT, exception.errorCode)
        assertEquals(now.plus(RETRY_DELAY), exception.retryAt)
    }

    @Test
    fun `상한에 닿았어도 자기 스트림을 대신하면 연다`() {
        limit.verifyOpenable(MAX_OPEN, true, now)
    }

    companion object {
        private const val MAX_OPEN = 2
        private val RETRY_DELAY: Duration = Duration.ofSeconds(30)
    }
}
