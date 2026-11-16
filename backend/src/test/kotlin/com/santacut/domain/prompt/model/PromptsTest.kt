package com.santacut.domain.prompt.model

import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PromptsTest {
    @Test
    fun `판이 없으면 비어 있다`() {
        assertTrue(Prompts(emptyList()).isEmpty())
    }

    @Test
    fun `받은 순서대로 바꾼다`() {
        val first = Prompt.initial(PromptName.SCORE, "1판", CREATED_AT)
        val second = first.next("2판", CREATED_AT)
        val prompts = Prompts(listOf(second, first))

        assertFalse(prompts.isEmpty())
        assertEquals(listOf(2, 1), prompts.map(Prompt::version))
    }

    companion object {
        private val CREATED_AT: Instant = Instant.parse("2026-11-16T00:00:00Z")
    }
}
