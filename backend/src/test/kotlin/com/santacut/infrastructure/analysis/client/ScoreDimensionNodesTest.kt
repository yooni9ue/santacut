package com.santacut.infrastructure.analysis.client

import org.junit.jupiter.api.Test
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScoreDimensionNodesTest {
    private val jsonMapper = JsonMapper.builder().build()

    @Test
    fun `항목 이름을 키로 한 객체는 항목 순서대로 점수를 읽는다`() {
        val scores = nodes("""{"cooperation_sharing":70,"care_empathy":80.5,"respectful_communication":60,"positivity_support":90}""")
        assertEquals(listOf(decimal("80.5"), decimal("90"), decimal("60"), decimal("70")), scores.toScoreValues())
    }

    @Test
    fun `항목 수와 같은 길이의 배열은 순서 그대로 읽는다`() {
        assertEquals(
            listOf(decimal("1"), decimal("2"), decimal("3"), decimal("4")),
            nodes("[1,2,3,4]").toScoreValues(),
        )
    }

    @Test
    fun `객체에 없는 항목과 숫자가 아닌 점수는 null이다`() {
        val scores = nodes("""{"care_empathy":"80","positivity_support":null,"respectful_communication":true}""")
        assertEquals(listOf<BigDecimal?>(null, null, null, null), scores.toScoreValues())
    }

    @Test
    fun `길이가 다른 배열이나 객체·배열이 아닌 노드는 빈 목록이 된다`() {
        assertTrue(nodes("[1,2,3]").toScoreValues().isEmpty())
        assertTrue(nodes("80").toScoreValues().isEmpty())
        assertTrue(nodes("\"scores\"").toScoreValues().isEmpty())
        assertTrue(ScoreDimensionNodes.of(jsonMapper.readTree("{}").path("missing")).toEvidenceIndices().isEmpty())
    }

    @Test
    fun `정상 근거 번호는 배열이든 숫자 하나든 정수 목록으로 읽는다`() {
        val evidence = nodes("""[[1,2],3,"4",[5.0]]""")
        assertEquals(listOf(listOf(1, 2), listOf(3), listOf(4), listOf(5)), evidence.toEvidenceIndices())
    }

    @Test
    fun `소수, 정수 범위를 넘는 수, 정수가 아닌 문자열은 범위 밖 번호가 된다`() {
        val evidence = nodes("""[[1.5],[1e20],["abc"],[true]]""")
        assertEquals(List(DIMENSION_COUNT) { listOf(INVALID_INDEX) }, evidence.toEvidenceIndices())
    }

    @Test
    fun `객체에 없는 항목과 null 근거는 범위 밖 번호가 된다`() {
        val evidence = nodes("""{"care_empathy":[1],"positivity_support":null,"respectful_communication":[2]}""")
        assertEquals(
            listOf(listOf(1), listOf(INVALID_INDEX), listOf(2), listOf(INVALID_INDEX)),
            evidence.toEvidenceIndices(),
        )
    }

    private fun nodes(json: String): ScoreDimensionNodes = ScoreDimensionNodes.of(readTree(json))

    private fun readTree(json: String): JsonNode = jsonMapper.readTree(json)

    private fun decimal(value: String): BigDecimal = BigDecimal(value)

    companion object {
        private const val INVALID_INDEX = -1
        private const val DIMENSION_COUNT = 4
    }
}
