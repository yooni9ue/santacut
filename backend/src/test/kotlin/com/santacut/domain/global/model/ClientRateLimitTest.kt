package com.santacut.domain.global.model

import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ClientRateLimitTest {
    private val limit = ClientRateLimit(MAX_REQUESTS, WINDOW)

    @Test
    fun `창 안에서 한도를 넘으면 CLIENT_RATE_LIMITED이고 retry_at은 창이 닫히는 시각이다`() {
        repeat(MAX_REQUESTS) { limit.acquire(CLIENT, NOW.plusSeconds(it.toLong())) }

        val exception = assertFailsWith<SantaCutException> { limit.acquire(CLIENT, NOW.plus(Duration.ofMinutes(30))) }
        assertEquals(ErrorCode.CLIENT_RATE_LIMITED, exception.errorCode)
        assertEquals(NOW.plus(WINDOW), exception.retryAt)
    }

    @Test
    fun `IP마다 따로 센다`() {
        repeat(MAX_REQUESTS) { limit.acquire(CLIENT, NOW) }

        assertDoesNotThrow { limit.acquire("198.51.100.2", NOW) }
    }

    @Test
    fun `창이 닫히면 다시 처음부터 센다`() {
        repeat(MAX_REQUESTS) { limit.acquire(CLIENT, NOW) }

        assertDoesNotThrow { repeat(MAX_REQUESTS) { limit.acquire(CLIENT, NOW.plus(WINDOW)) } }
    }

    @Test
    fun `IP를 모르는 호출(HTTP 밖)은 세지 않는다`() {
        assertDoesNotThrow { repeat(MAX_REQUESTS + 1) { limit.acquire(null, NOW) } }
    }

    companion object {
        private const val MAX_REQUESTS = 2
        private const val CLIENT = "198.51.100.1"
        private val WINDOW: Duration = Duration.ofHours(1)
        private val NOW: Instant = Instant.parse("2026-12-01T00:00:00Z")
    }
}
