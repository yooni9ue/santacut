package com.santacut.domain.analysis.model

import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProviderBackoffTest {
    private val now = Instant.parse("2026-11-16T03:00:00Z")
    private val backoff = ProviderBackoff(PAUSE, MAX_FAILURES)

    @Test
    fun `제공자가 막으면 바로 멈추고 멈춤 시간이 지나면 다시 잡는다`() {
        backoff.pauseFrom(now)

        assertTrue(backoff.isPausedAt(now.plus(PAUSE).minusSeconds(1)))
        assertFalse(backoff.isPausedAt(now.plus(PAUSE)))
    }

    @Test
    fun `3번 연달아 실패해야 멈춘다`() {
        backoff.recordFailure(now)
        backoff.recordFailure(now)
        assertFalse(backoff.isPausedAt(now))

        backoff.recordFailure(now)

        assertTrue(backoff.isPausedAt(now))
    }

    @Test
    fun `중간에 성공하면 실패 횟수를 처음부터 센다`() {
        backoff.recordFailure(now)
        backoff.recordFailure(now)
        backoff.recordSuccess()
        backoff.recordFailure(now)

        assertFalse(backoff.isPausedAt(now))
    }

    companion object {
        private val PAUSE: Duration = Duration.ofMinutes(15)
        private const val MAX_FAILURES = 3
    }
}
