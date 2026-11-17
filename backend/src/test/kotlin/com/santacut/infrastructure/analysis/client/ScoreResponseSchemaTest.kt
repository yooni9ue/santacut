package com.santacut.infrastructure.analysis.client

import com.santacut.domain.analysis.model.ScoreDimension
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import kotlin.test.assertEquals

/**
 * 채점 응답 스키마가 읽는 쪽(ScoreClient)이 기대하는 키와 타입을 모두 필수로 요구하는지 확인한다.
 */
class ScoreResponseSchemaTest {
    private val schema = JsonMapper.builder().build().readTree(ScoreResponseSchema.JSON)

    @Test
    fun `sufficient, 모든 항목 키, playfulness를 필수로 요구한다`() {
        val expected = listOf("sufficient") + ScoreDimension.entries.map { it.key } + "playfulness"

        assertEquals(expected, schema.path("required").values().map { it.asString() })
    }

    @Test
    fun `항목마다 근거 배열 e, 기회 수 n, 점수 s를 정수로 요구한다`() {
        ScoreDimension.entries.forEach { dimension ->
            val node = schema.path("properties").path(dimension.key)

            assertEquals(listOf("e", "n", "s"), node.path("required").values().map { it.asString() })
            assertEquals("integer", node.path("properties").path("e").path("items").path("type").asString())
            assertEquals("integer", node.path("properties").path("n").path("type").asString())
            assertEquals("integer", node.path("properties").path("s").path("type").asString())
        }
    }
}
