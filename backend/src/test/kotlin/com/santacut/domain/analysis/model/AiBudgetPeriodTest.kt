package com.santacut.domain.analysis.model

import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals

/**
 * 비용 한도 기간이 한국 시각 자정과 달이 바뀌는 때를 경계로 나뉘는지 확인한다.
 */
class AiBudgetPeriodTest {
    @Test
    fun `한국 시각 자정 직전까지는 전날 기간이다`() {
        val period = AiBudgetPeriod.at(Instant.parse("2026-11-16T14:59:59Z"))

        assertEquals("2026-11-16", period.dayKey())
        assertEquals("2026-11-01", period.monthStartKey())
    }

    @Test
    fun `한국 시각 자정부터 다음 날 기간이다`() {
        val period = AiBudgetPeriod.at(Instant.parse("2026-11-16T15:00:00Z"))

        assertEquals("2026-11-17", period.dayKey())
    }

    @Test
    fun `한국 시각으로 달이 바뀌면 한 달 기간이 새 달 1일부터 다시 시작한다`() {
        val lastDay = AiBudgetPeriod.at(Instant.parse("2026-11-30T14:59:59Z"))
        val firstDay = AiBudgetPeriod.at(Instant.parse("2026-11-30T15:00:00Z"))

        assertEquals("2026-11-01", lastDay.monthStartKey())
        assertEquals("2026-12-01", firstDay.dayKey())
        assertEquals("2026-12-01", firstDay.monthStartKey())
    }
}
