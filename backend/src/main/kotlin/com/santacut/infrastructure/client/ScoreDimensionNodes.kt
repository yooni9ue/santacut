package com.santacut.infrastructure.client

import com.santacut.domain.model.ScoreDimension
import tools.jackson.databind.JsonNode
import java.math.BigDecimal

/**
 * AI 응답의 scores·evidence 노드를 [ScoreDimension] 순서로 나눈 항목별 노드 목록.
 */
class ScoreDimensionNodes(private val values: List<JsonNode>) {
    /** 항목별 점수. 숫자가 아닌 항목은 null이다. */
    fun toScoreValues(): List<BigDecimal?> = values.map { if (it.isNumber) it.decimalValue() else null }

    /** 항목별 근거 번호. 배열이 아닌 노드는 번호 하나로 본다. */
    fun toEvidenceIndices(): List<List<Int>> =
        values.map { node -> (if (node.isArray) elementsOf(node) else listOf(node)).map(::toEvidenceIndex) }

    companion object {
        private const val STRING_NODE_TYPE = "STRING"
        private const val INVALID_INDEX = -1

        /** 항목 이름을 키로 한 객체나 항목 순서의 배열을 모두 받아들인다. */
        fun of(node: JsonNode): ScoreDimensionNodes = ScoreDimensionNodes(
            when {
                node.isObject -> ScoreDimension.entries.map { node.path(it.key) }
                node.isArray && node.size() == ScoreDimension.entries.size -> elementsOf(node)
                else -> emptyList()
            },
        )

        private fun elementsOf(node: JsonNode): List<JsonNode> = (0 until node.size()).map(node::get)

        /** 정수로 읽을 수 없는 근거 번호는 범위 밖 값(-1)으로 바꿔 도메인 검증에서 걸러지게 한다. */
        private fun toEvidenceIndex(node: JsonNode): Int = when {
            node.isIntegralNumber -> node.asInt()
            node.isNumber && node.decimalValue().stripTrailingZeros().scale() <= 0 ->
                runCatching { node.decimalValue().intValueExact() }.getOrDefault(INVALID_INDEX)
            node.nodeType.name == STRING_NODE_TYPE -> node.toString().trim('"').toIntOrNull() ?: INVALID_INDEX
            else -> INVALID_INDEX
        }
    }
}
