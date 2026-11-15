package com.santacut.domain.global.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CoordinateTest {
    @Test
    fun `위도 ±90도와 경도 ±180도 경계는 만들 수 있다`() {
        assertEquals(90.0, Coordinate(90.0, 180.0).latitude)
        assertEquals(-180.0, Coordinate(-90.0, -180.0).longitude)
    }

    @Test
    fun `범위를 벗어난 위도와 경도는 거절한다`() {
        assertFailsWith<IllegalArgumentException> { Coordinate(91.0, 0.0) }
        assertFailsWith<IllegalArgumentException> { Coordinate(-91.0, 0.0) }
        assertFailsWith<IllegalArgumentException> { Coordinate(0.0, 181.0) }
        assertFailsWith<IllegalArgumentException> { Coordinate(0.0, -181.0) }
    }

    @Test
    fun `무한대와 NaN은 거절한다`() {
        assertFailsWith<IllegalArgumentException> { Coordinate(Double.POSITIVE_INFINITY, 0.0) }
        assertFailsWith<IllegalArgumentException> { Coordinate(0.0, Double.NEGATIVE_INFINITY) }
        assertFailsWith<IllegalArgumentException> { Coordinate(0.0, Double.NaN) }
    }
}
