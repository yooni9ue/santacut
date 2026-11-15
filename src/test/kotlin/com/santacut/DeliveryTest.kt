package com.santacut

import com.santacut.core.*
import org.junit.jupiter.api.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.*

class DeliveryTest {
    private val start = ZonedDateTime.of(2026,12,25,0,0,0,0,ZoneId.of("Asia/Seoul"))
    @Test fun `work pauses after six hours and resumes next midnight`() {
        assertEquals(start.plusHours(6),Delivery.arrival(start,21600.0))
        assertEquals(start.plusDays(1).plusSeconds(1),Delivery.arrival(start,21600.01))
        assertEquals(start.plusDays(1).plusMinutes(3),Delivery.arrival(start,121*180.0))
    }
    @Test fun `year rollover and accumulated travel`() {
        assertEquals(start.plusDays(1).plusMinutes(6),Delivery.arrival(start,6*3600.0+360))
        assertEquals(2027,Delivery.arrival(start,8*21600.0).year)
    }
    @Test fun `great circle distance respects identity and antipodes`() {
        assertEquals(0.0,Delivery.distance(Coordinate(37.5,127.0),Coordinate(37.5,127.0)))
        assertEquals(20015.1144,Delivery.distance(Coordinate(0.0,0.0),Coordinate(0.0,180.0)),0.01)
        assertFailsWith<IllegalArgumentException> { Coordinate(Double.NaN,0.0) }
    }
    @Test fun `share markup escapes executable text and attributes`() {
        assertEquals("&lt;script&gt;&amp;&quot;&#39;",escapeHtml("<script>&\"'"))
    }
}
