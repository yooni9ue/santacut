package com.santacut.infrastructure.roast.client

import com.google.genai.types.Schema
import com.santacut.domain.roast.model.RoastTextKind
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 작가 응답 스키마(프롬프트 5판)가 Gemini 스키마 형식으로 읽히고, 문장 필드마다 서버 검증과 같은 글자 수 상한을 싣는지 확인한다.
 */
class RoastResponseSchemaTest {
    private val schema: Schema = Schema.fromJson(RoastResponseSchema.JSON)
    private val properties = schema.properties().orElseThrow()

    @Test
    fun `판정 한 줄, 근거 라벨과 한 줄, 결론 칭호와 마지막 한 줄에 검증과 같은 글자 수 상한을 싣는다`() {
        val evidence = properties.getValue("evidence").items().orElseThrow().properties().orElseThrow()
        val conclusion = properties.getValue("conclusion").properties().orElseThrow()

        assertMaxLength(RoastTextKind.VERDICT_LINE, properties.getValue("verdict_line"))
        assertMaxLength(RoastTextKind.LABEL, evidence.getValue("label"))
        assertMaxLength(RoastTextKind.EVIDENCE_LINE, evidence.getValue("line"))
        assertMaxLength(RoastTextKind.TITLE, conclusion.getValue("title"))
        assertMaxLength(RoastTextKind.FINAL_LINE, conclusion.getValue("final_line"))
        assertTrue(evidence.getValue("chip_key").maxLength().isEmpty)
    }

    @Test
    fun `세 묶음과 근거·결론의 키를 모두 필수로 요구하고 농담 빼고 보기 필드는 없다`() {
        assertEquals(listOf("verdict_line", "evidence", "conclusion"), schema.required().orElseThrow())
        assertEquals(
            listOf("kind", "line_id", "chip_key", "label", "line"),
            properties.getValue("evidence").items().orElseThrow().required().orElseThrow(),
        )
        assertEquals(listOf("title", "hero_line_id", "hero_chip_key", "final_line"), properties.getValue("conclusion").required().orElseThrow())
        assertTrue("verdict_mild" !in RoastResponseSchema.JSON)
    }

    private fun assertMaxLength(kind: RoastTextKind, field: Schema) {
        assertEquals(kind.maxLength.toLong(), field.maxLength().orElseThrow())
        assertTrue(field.description().orElseThrow().contains("${kind.minLength}~${kind.maxLength}자"))
    }
}
