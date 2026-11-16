package com.santacut.application.prompt.result

import com.santacut.domain.prompt.model.Prompt
import com.santacut.domain.prompt.model.PromptName
import com.santacut.domain.prompt.model.Prompts
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PromptVersionResultsTest {
    private val createdAt = Instant.parse("2026-11-16T00:00:00Z")

    @Test
    fun `판 목록 순서를 지키고 본문 없이 이름, 판 번호, 저장 시각만 담는다`() {
        val first = Prompt.initial(PromptName.SCORE, "1판", createdAt)
        val second = first.next("2판", createdAt.plusSeconds(60))

        val results = PromptVersionResults.from(Prompts(listOf(second, first)))

        assertEquals(
            listOf(PromptVersionResult("score", 2, createdAt.plusSeconds(60)), PromptVersionResult("score", 1, createdAt)),
            results.map { it },
        )
    }

    @Test
    fun `판이 없으면 결과도 비어 있다`() {
        assertTrue(PromptVersionResults.from(Prompts(emptyList())).map { it }.isEmpty())
    }
}
