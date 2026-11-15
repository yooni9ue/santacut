package com.santacut.domain.analysis.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class MojibakeTextTest {
    @Test
    fun `UTF-8 바이트를 한 글자씩 적은 문자열을 되살린다`() {
        val broken = String("산타 🎅".toByteArray(Charsets.UTF_8), Charsets.ISO_8859_1)
        assertEquals("산타 🎅", MojibakeText.repair(broken))
    }

    @Test
    fun `이미 올바른 문자열과 ASCII는 그대로 둔다`() {
        assertEquals("산타", MojibakeText.repair("산타"))
        assertEquals("santa", MojibakeText.repair("santa"))
    }

    @Test
    fun `다시 읽으면 올바른 UTF-8이 아닌 라틴 문자열은 그대로 둔다`() {
        assertEquals("café", MojibakeText.repair("café"))
    }
}
