package com.santacut.domain.ranking.model

import com.santacut.domain.global.model.Coordinate
import org.junit.jupiter.api.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DeliveryTest {
    private val start = ZonedDateTime.of(2026, 12, 25, 0, 0, 0, 0, ZoneId.of("Asia/Seoul"))

    @Test
    fun `여섯 시간 일하면 멈추고 다음 날 자정에 다시 시작한다`() {
        assertEquals(start.plusHours(6), Delivery.arrival(start, 21600.0))
        assertEquals(start.plusDays(1).plusSeconds(1), Delivery.arrival(start, 21600.01))
        assertEquals(start.plusDays(1).plusMinutes(3), Delivery.arrival(start, 121 * 180.0))
    }

    @Test
    fun `누적 작업 시간이 길면 해를 넘겨 도착한다`() {
        assertEquals(start.plusDays(1).plusMinutes(6), Delivery.arrival(start, 6 * 3600.0 + 360))
        assertEquals(2027, Delivery.arrival(start, 8 * 21600.0).year)
    }

    @Test
    fun `대원 거리는 같은 지점에서 0이고 대척점에서 지구 반 바퀴다`() {
        assertEquals(0.0, Delivery.distance(Coordinate(37.5, 127.0), Coordinate(37.5, 127.0)))
        assertEquals(20015.1144, Delivery.distance(Coordinate(0.0, 0.0), Coordinate(0.0, 180.0)), 0.01)
        assertFailsWith<IllegalArgumentException> { Coordinate(Double.NaN, 0.0) }
    }

    @Test
    fun `한 구간 작업 시간은 시속 100km 이동 시간에 정거장 3분을 더한 값이다`() {
        val seoul = Coordinate(37.5, 127.0)
        assertEquals(180.0, Delivery.legSeconds(seoul, seoul))
        val equatorStart = Coordinate(0.0, 0.0)
        val equatorEnd = Coordinate(0.0, 1.0)
        val expected = Delivery.distance(equatorStart, equatorEnd) / 100 * 3600 + 180
        assertEquals(expected, Delivery.legSeconds(equatorStart, equatorEnd))
    }

    @Test
    fun `작업 시간이 0 이하이거나 유한하지 않으면 도착 시각을 계산하지 않는다`() {
        assertFailsWith<IllegalArgumentException> { Delivery.arrival(start, 0.0) }
        assertFailsWith<IllegalArgumentException> { Delivery.arrival(start, Double.POSITIVE_INFINITY) }
    }
}
