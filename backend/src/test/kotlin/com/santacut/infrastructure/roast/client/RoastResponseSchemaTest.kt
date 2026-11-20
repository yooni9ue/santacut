package com.santacut.infrastructure.roast.client

import com.google.genai.types.Schema
import com.santacut.domain.roast.model.RoastTextKind
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 작가 응답 스키마가 Gemini 스키마 형식으로 읽히고, 문장 필드마다 서버 검증과 같은 글자 수 상한을 싣는지 확인한다.
 */
class RoastResponseSchemaTest {
    private val schema: Schema = Schema.fromJson(RoastResponseSchema.JSON)
    private val properties = schema.properties().orElseThrow()

    @Test
    fun `칭호, 판결, 담백 판결, 쪽지에 검증과 같은 글자 수 상한과 범위 설명을 싣는다`() {
        assertMaxLength(RoastTextKind.TITLE, properties.getValue("title"))
        assertMaxLength(RoastTextKind.VERDICT, properties.getValue("verdict"))
        assertMaxLength(RoastTextKind.VERDICT_MILD, properties.getValue("verdict_mild"))
        assertMaxLength(RoastTextKind.LETTER, properties.getValue("letter"))
    }

    @Test
    fun `짝의 놀림과 뒤집기, 줄거리 줄에도 글자 수 상한을 싣고 사실 키에는 싣지 않는다`() {
        val pair = properties.getValue("pairs").items().orElseThrow().properties().orElseThrow()
        val story = properties.getValue("story").items().orElseThrow().properties().orElseThrow()

        assertMaxLength(RoastTextKind.ROAST, pair.getValue("roast"))
        assertMaxLength(RoastTextKind.FLIP, pair.getValue("flip"))
        assertMaxLength(RoastTextKind.STORY, story.getValue("line"))
        assertTrue(pair.getValue("fact_key").maxLength().isEmpty)
    }

    @Test
    fun `모든 키를 필수로 요구하고 짝은 다섯 개, 줄거리는 세 줄이다`() {
        assertEquals(listOf("title", "verdict", "verdict_mild", "pairs", "letter", "story"), schema.required().orElseThrow())
        assertEquals(PAIRS, properties.getValue("pairs").maxItems().orElseThrow())
        assertEquals(STORY_LINES, properties.getValue("story").minItems().orElseThrow())
    }

    private fun assertMaxLength(kind: RoastTextKind, field: Schema) {
        assertEquals(kind.maxLength.toLong(), field.maxLength().orElseThrow())
        assertTrue(field.description().orElseThrow().contains("${kind.minLength}~${kind.maxLength}자"))
    }

    companion object {
        private const val PAIRS = 5L
        private const val STORY_LINES = 3L
    }
}
