package com.santacut.domain.roast.model

import tools.jackson.databind.JsonNode

/** 작가 응답 JSON을 느슨하게 읽는 도구. 빠졌거나 문자열이 아닌 값은 빈 문자열로 읽어 검증에서 떨어지게 한다. */
object RoastJson {
    fun textOf(node: JsonNode, field: String): String {
        val value = node.path(field)
        return if (value.isString) value.asString() else ""
    }

    fun intOf(node: JsonNode, field: String): Int? {
        val value = node.path(field)
        return if (value.isInt) value.asInt() else null
    }
}
