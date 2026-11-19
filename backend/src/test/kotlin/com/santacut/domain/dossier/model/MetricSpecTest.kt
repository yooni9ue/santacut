package com.santacut.domain.dossier.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class MetricSpecTest {
    private val spec = MetricSpec("laugh_density", "ㅋ 밀도", "per100", 10.0, 40.0, 150.0, 30, true)

    @Test
    fun `백분위는 p10·p50·p90 사이를 선형으로 잇고 양 끝은 0과 100으로 자른다`() {
        assertEquals(listOf(10.0, 50.0, 90.0), listOf(10.0, 40.0, 150.0).map(spec::percentileOf))
        assertEquals(30.0, spec.percentileOf(25.0))
        assertEquals(70.0, spec.percentileOf(95.0))
        assertEquals(0.0, spec.percentileOf(0.0))
        assertEquals(100.0, spec.percentileOf(10_000.0))
    }

    @Test
    fun `값은 unit에 맞춰 서버가 포맷한다`() {
        assertEquals("100개당 212개", spec.format(212.0))
        assertEquals("23%", spec.copy(unit = "ratio").format(0.234))
        assertEquals("2시간 40분대", spec.copy(unit = "minutes").format(161.0))
        assertEquals("새벽 1시대", spec.copy(unit = "hour").format(1.0))
        assertEquals("3.8개", spec.copy(unit = "count").format(3.84))
        assertEquals("31자", spec.copy(unit = "chars").format(31.0))
        assertEquals("120일", spec.copy(unit = "days").format(120.0))
        assertEquals("3.5배", spec.copy(unit = "times").format(3.5))
    }

    @Test
    fun `모르는 unit은 위반이다`() {
        assertEquals(1, spec.copy(unit = "furlong").violations().size)
    }
}
