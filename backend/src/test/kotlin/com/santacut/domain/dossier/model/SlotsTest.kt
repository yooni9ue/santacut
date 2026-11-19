package com.santacut.domain.dossier.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SlotsTest {
    private val slots = Slots.of("phrase" to "진짜", "count" to "12번", "value" to "100개당 3개")

    @Test
    fun `조사 슬롯은 값 없이 그 값에 맞는 조사만 쓴다`() {
        assertEquals("'진짜'를 12번 찍음", slots.fill("'{phrase}'{phrase:을} {count} 찍음"))
        assertEquals("12번이 증거", slots.fill("{count}{count:가} 증거"))
        assertEquals("100개당 3개를 풂", slots.fill("{value}{value:을} 풂"))
    }

    @Test
    fun `값이 없는 슬롯이 있으면 채우지 않는다`() {
        assertFalse(slots.canFill("{phrase} {topic}"))
        assertNull(slots.fillOrNull("{topic}"))
        assertTrue(slots.canFill("{phrase}{phrase:로}"))
    }

    @Test
    fun `null 값은 슬롯에서 빠지고 합치면 뒤 값이 이긴다`() {
        val merged = Slots.of("phrase" to null, "count" to "1번") + Slots.of("count" to "2번")

        assertFalse(merged.canFill("{phrase}"))
        assertEquals("2번", merged.fill("{count}"))
    }
}
