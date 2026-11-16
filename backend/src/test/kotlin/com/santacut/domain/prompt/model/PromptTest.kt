package com.santacut.domain.prompt.model

import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class PromptTest {
    @Test
    fun `첫 판은 1판이다`() {
        val prompt = Prompt.initial(NAME, "본문", CREATED_AT)

        assertEquals(NAME.value, prompt.name)
        assertEquals(Prompt.FIRST_VERSION, prompt.version)
        assertEquals("본문", prompt.content)
        assertEquals(CREATED_AT, prompt.createdAt)
    }

    @Test
    fun `다음 판은 같은 이름에 판 번호가 1 크고 ID가 새로 붙는다`() {
        val first = Prompt.initial(NAME, "본문", CREATED_AT)
        val later = CREATED_AT.plusSeconds(60)

        val next = first.next("새 본문", later)

        assertEquals(NAME.value, next.name)
        assertEquals(first.version + 1, next.version)
        assertEquals("새 본문", next.content)
        assertEquals(later, next.createdAt)
        assertNotEquals(first.id, next.id)
    }

    @Test
    fun `다음 판을 만들어도 원래 판은 바뀌지 않는다`() {
        val first = Prompt.initial(NAME, "본문", CREATED_AT)

        first.next("새 본문", CREATED_AT)

        assertEquals(Prompt.FIRST_VERSION, first.version)
        assertEquals("본문", first.content)
    }

    companion object {
        private val NAME: PromptName = PromptName.SCORE
        private val CREATED_AT: Instant = Instant.parse("2026-11-16T00:00:00Z")
    }
}
