package com.santacut.domain.global.model

import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals

class RequestWindowsTest {
    @Test
    fun `같은 창 안의 요청을 차례로 세고 창이 닫히면 새 창을 연다`() {
        val windows = RequestWindows()

        assertEquals(1, windows.record(CLIENT, NOW, WINDOW).count)
        assertEquals(2, windows.record(CLIENT, NOW.plusSeconds(1), WINDOW).count)
        val reopened = windows.record(CLIENT, NOW.plus(WINDOW), WINDOW)
        assertEquals(RequestWindow(NOW.plus(WINDOW), 1), reopened)
    }

    @Test
    fun `닫힌 창만 지운다`() {
        val windows = RequestWindows()
        windows.record(CLIENT, NOW, WINDOW)
        windows.record("198.51.100.2", NOW.plus(WINDOW), WINDOW)

        windows.evictOverAt(NOW.plus(WINDOW), WINDOW)

        assertEquals(1, windows.size())
    }

    companion object {
        private const val CLIENT = "198.51.100.1"
        private val WINDOW: Duration = Duration.ofHours(1)
        private val NOW: Instant = Instant.parse("2026-12-01T00:00:00Z")
    }
}
