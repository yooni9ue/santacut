package com.santacut.presentation.prompt.response

import com.santacut.application.prompt.result.PromptVersionResult
import com.santacut.application.prompt.result.PromptVersionResults
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.Instant
import kotlin.test.assertEquals

class PromptVersionResponsesTest {
    private val mapper = jacksonObjectMapper()
    private val createdAt = Instant.parse("2026-11-16T03:00:00Z")

    @Test
    fun `판 목록 응답은 순서를 지킨 JSON 배열 하나로 나가고 본문이 없다`() {
        val results = PromptVersionResults(
            listOf(PromptVersionResult("score", 2, createdAt), PromptVersionResult("score", 1, createdAt)),
        )

        val json = mapper.writeValueAsString(PromptVersionResponses.from(results))

        assertEquals(
            """[{"name":"score","version":2,"created_at":"2026-11-16T03:00:00Z"},""" +
                """{"name":"score","version":1,"created_at":"2026-11-16T03:00:00Z"}]""",
            json,
        )
    }

    @Test
    fun `판이 없으면 빈 JSON 배열이다`() {
        assertEquals("[]", mapper.writeValueAsString(PromptVersionResponses.from(PromptVersionResults(emptyList()))))
    }
}
