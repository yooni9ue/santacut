package com.santacut.domain.model

import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UtcDatabaseTimeTest {
    @Test
    fun `UTC 시각을 밀리초까지의 데이터베이스 문자열로 바꾸고 다시 읽으면 같은 시각이다`() {
        val instant = Instant.parse("2026-12-01T01:02:03.004Z")
        assertEquals("2026-12-01 01:02:03.004", UtcDatabaseTime.format(instant))
        assertEquals(instant, UtcDatabaseTime.parse(UtcDatabaseTime.format(instant)))
        assertEquals("2026-01-02 00:00:00.000", UtcDatabaseTime.format(Instant.parse("2026-01-02T00:00:00Z")))
    }

    @Test
    fun `밀리초보다 작은 단위는 버린다`() {
        val instant = Instant.parse("2026-12-01T01:02:03.004999999Z")
        assertEquals("2026-12-01 01:02:03.004", UtcDatabaseTime.format(instant))
        assertEquals(instant.truncatedTo(ChronoUnit.MILLIS), UtcDatabaseTime.parse(UtcDatabaseTime.format(instant)))
    }

    @Test
    fun `문자열 정렬 순서가 시간 순서와 같다`() {
        val instants = listOf(
            Instant.parse("2027-01-01T00:00:00Z"),
            Instant.parse("2026-12-31T23:59:59.999Z"),
            Instant.parse("2026-01-01T00:00:00.001Z"),
            Instant.parse("2026-12-31T23:59:59.998Z"),
            Instant.parse("2026-02-01T09:00:00Z"),
        )
        val byText = instants.sortedBy(UtcDatabaseTime::format)
        assertEquals(instants.sorted(), byText)
    }

    @Test
    fun `변환기는 null을 null로 주고받고 값은 같은 형식으로 저장한다`() {
        val converter = UtcInstantConverter()
        assertNull(converter.convertToDatabaseColumn(null))
        assertNull(converter.convertToEntityAttribute(null))
        val instant = Instant.parse("2026-12-01T01:02:03.004Z")
        assertEquals("2026-12-01 01:02:03.004", converter.convertToDatabaseColumn(instant))
        assertEquals(instant, converter.convertToEntityAttribute("2026-12-01 01:02:03.004"))
    }
}
