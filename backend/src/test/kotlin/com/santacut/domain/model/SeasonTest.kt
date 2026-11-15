package com.santacut.domain.model

import com.santacut.domain.exception.ErrorCode
import com.santacut.domain.exception.SantaCutException
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SeasonTest {
    private val season = Season(2026)
    private val christmasInSeoul = Instant.parse("2026-12-24T15:00:00Z")

    @Test
    fun `서울 시각 12월 25일 0시에 배송을 시작한다`() {
        assertEquals(christmasInSeoul, season.deliveryStart().toInstant())
    }

    @Test
    fun `배송 시작 순간부터 시즌이 마감된다`() {
        assertFalse(season.isClosedAt(christmasInSeoul.minusMillis(1)))
        assertTrue(season.isClosedAt(christmasInSeoul))
        season.verifyOpenAt(christmasInSeoul.minusMillis(1))
        val exception = assertFailsWith<SantaCutException> { season.verifyOpenAt(christmasInSeoul) }
        assertEquals(ErrorCode.SEASON_FROZEN, exception.errorCode)
    }
}
