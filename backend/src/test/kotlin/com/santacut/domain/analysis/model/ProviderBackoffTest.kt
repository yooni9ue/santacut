package com.santacut.domain.analysis.model

import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProviderBackoffTest {
    private val now = Instant.parse("2026-11-16T03:00:00Z")
    private val backoff = ProviderBackoff(PAUSE, MAX_FAILURES)

    @Test
    fun `제공자가 막으면 바로 멈추고 멈춤 시간이 지나면 다시 잡는다`() {
        backoff.record(ScoringFailure.PROVIDER_BLOCKED, now)

        assertTrue(backoff.isPausedAt(now.plus(PAUSE).minusSeconds(1)))
        assertFalse(backoff.isPausedAt(now.plus(PAUSE)))
    }

    @Test
    fun `분석 오류가 3번 연달아 나야 멈춘다`() {
        repeat(MAX_FAILURES - 1) { backoff.record(ScoringFailure.ANALYSIS_ERROR, now) }
        assertFalse(backoff.isPausedAt(now))

        backoff.record(ScoringFailure.ANALYSIS_ERROR, now)

        assertTrue(backoff.isPausedAt(now))
    }

    @Test
    fun `근거 부족은 몇 번이 나도 연속 실패로 세지 않는다`() {
        repeat(MAX_FAILURES * 2) { backoff.record(ScoringFailure.INSUFFICIENT_EVIDENCE, now) }

        assertFalse(backoff.isPausedAt(now))
    }

    @Test
    fun `입력 탓 응답 오류는 몇 번이 나도 연속 실패로 세지 않는다`() {
        repeat(MAX_FAILURES * 2) { backoff.record(ScoringFailure.INPUT_REJECTED, now) }

        assertFalse(backoff.isPausedAt(now))
    }

    @Test
    fun `중간에 성공하면 실패 횟수를 처음부터 센다`() {
        repeat(MAX_FAILURES - 1) { backoff.record(ScoringFailure.ANALYSIS_ERROR, now) }
        backoff.recordSuccess()
        backoff.record(ScoringFailure.ANALYSIS_ERROR, now)

        assertFalse(backoff.isPausedAt(now))
    }

    @Test
    fun `예외를 실패 종류로 나눈다`() {
        assertEquals(ScoringFailure.PROVIDER_BLOCKED, ScoringFailure.of(SantaCutException(ErrorCode.AI_PROVIDER_UNAVAILABLE)))
        assertEquals(ScoringFailure.INSUFFICIENT_EVIDENCE, ScoringFailure.of(SantaCutException(ErrorCode.INSUFFICIENT_EVIDENCE)))
        assertEquals(ScoringFailure.INPUT_REJECTED, ScoringFailure.of(SantaCutException(ErrorCode.AI_RESPONSE_INVALID)))
        assertEquals(ScoringFailure.INPUT_REJECTED, ScoringFailure.of(SantaCutException(ErrorCode.AI_RESPONSE_SCHEMA_INVALID)))
        assertEquals(ScoringFailure.INPUT_REJECTED, ScoringFailure.of(SantaCutException(ErrorCode.AI_SCORE_OUT_OF_RANGE)))
        assertEquals(ScoringFailure.ANALYSIS_ERROR, ScoringFailure.of(SantaCutException(ErrorCode.ANALYSIS_PAYLOAD_MISSING)))
        assertEquals(ScoringFailure.ANALYSIS_ERROR, ScoringFailure.of(IllegalStateException()))
    }

    companion object {
        private val PAUSE: Duration = Duration.ofMinutes(15)
        private const val MAX_FAILURES = 3
    }
}
