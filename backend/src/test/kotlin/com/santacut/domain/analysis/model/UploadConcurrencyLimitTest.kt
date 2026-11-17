package com.santacut.domain.analysis.model

import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UploadConcurrencyLimitTest {
    private val limit = UploadConcurrencyLimit(1, RETRY_DELAY)

    @Test
    fun `자리가 차 있으면 기다리지 않고 UPLOAD_BUSY이고 retry_at을 알려 준다`() {
        val exception = limit.runWithinLimit(NOW) {
            assertFailsWith<SantaCutException> { limit.runWithinLimit(NOW) { } }
        }

        assertEquals(ErrorCode.UPLOAD_BUSY, exception.errorCode)
        assertEquals(NOW.plus(RETRY_DELAY), exception.retryAt)
    }

    @Test
    fun `작업이 끝나면 실패했어도 자리를 돌려준다`() {
        assertFailsWith<IllegalStateException> { limit.runWithinLimit(NOW) { throw IllegalStateException() } }

        assertEquals(VALUE, limit.runWithinLimit(NOW) { VALUE })
    }

    companion object {
        private const val VALUE = "저장"
        private val RETRY_DELAY: Duration = Duration.ofSeconds(10)
        private val NOW: Instant = Instant.parse("2026-12-01T00:00:00Z")
    }
}
