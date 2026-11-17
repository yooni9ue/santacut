package com.santacut.domain.ranking.model

import org.junit.jupiter.api.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeliveryScheduleTest {
    private val start = ZonedDateTime.of(2026, 12, 25, 0, 0, 0, 0, ZoneId.of("Asia/Seoul"))

    @Test
    fun `배송 기간 안에 끝나면 작업 시간을 줄이지 않는다`() {
        val schedule = DeliverySchedule.fit(start, 3 * 21600.0, 7)
        assertFalse(schedule.isCompressed())
        assertEquals(start.plusDays(1).plusSeconds(60), schedule.arrival(21660.0))
    }

    @Test
    fun `배송 기간을 넘으면 같은 비율로 줄여 마지막 참가자가 마지막 날 6시에 받는다`() {
        val schedule = DeliverySchedule.fit(start, 70 * 21600.0, 7)
        assertTrue(schedule.isCompressed())
        assertEquals(start.plusDays(6).plusHours(6), schedule.arrival(70 * 21600.0))
        assertEquals(start.plusHours(3), schedule.arrival(5 * 21600.0))
    }
}
