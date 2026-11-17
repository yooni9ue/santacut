package com.santacut.domain.participant.model

import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class RegionChangeLimitTest {
    private val limit = RegionChangeLimit(2, Duration.ofHours(24), 5)
    private val oldest = Instant.parse("2026-12-01T00:00:00Z")

    @Test
    fun `창 안 변경이 한도보다 적으면 통과한다`() {
        limit.verify(seasonChanges = 4, recentChanges = 1, oldestRecentAt = oldest)
    }

    @Test
    fun `창 안 한도에 닿으면 가장 오래된 변경이 창을 벗어나는 시각을 retry_at으로 알린다`() {
        val exception = assertFailsWith<SantaCutException> { limit.verify(2, 2, oldest) }
        assertEquals(ErrorCode.REGION_CHANGE_LIMITED, exception.errorCode)
        assertEquals(oldest.plus(Duration.ofHours(24)), exception.retryAt)
    }

    @Test
    fun `시즌 한도에 닿으면 다시 시도할 시각 없이 거절한다`() {
        val exception = assertFailsWith<SantaCutException> { limit.verify(5, 0, null) }
        assertEquals(ErrorCode.REGION_CHANGE_SEASON_LIMIT, exception.errorCode)
        assertNull(exception.retryAt)
    }

    @Test
    fun `창 시작은 지금에서 창 길이만큼 앞이다`() {
        assertEquals(oldest.minus(Duration.ofHours(24)), limit.windowStart(oldest))
    }
}
