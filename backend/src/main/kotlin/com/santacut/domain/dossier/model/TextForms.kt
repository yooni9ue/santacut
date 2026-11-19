package com.santacut.domain.dossier.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * 본인 메시지에서 셀 표면형 목록. 두 글자 이상은 부분 문자열로 세고, 한 글자는 앞뒤에 글자·숫자가 붙지 않은 낱말일 때만 센다.
 * 겹쳐 나온 자리는 한 번만 센다. JSON에서는 문자열 배열 하나로 읽는다.
 */
class TextForms @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(
    private val values: List<String>,
) {
    @JsonValue
    fun toList(): List<String> = values.toList()

    /** 표면형이 하나도 없거나 빈 표면형이 섞여 있는지. */
    fun isMalformed(): Boolean = values.isEmpty() || values.any { form -> form.isBlank() }

    /** [text] 안에 나온 모든 표면형의 횟수 합. */
    fun countIn(text: String): Int = values.sumOf { form ->
        if (form.length < MIN_SUBSTRING_LENGTH) tokenCount(text, form) else substringCount(text, form, 0, 0)
    }

    private fun tokenCount(text: String, form: String): Int =
        Regex(TOKEN_START + Regex.escape(form) + TOKEN_END).findAll(text).count()

    private tailrec fun substringCount(text: String, form: String, from: Int, found: Int): Int {
        val index = text.indexOf(form, from)
        if (index < 0) return found
        return substringCount(text, form, index + form.length, found + 1)
    }

    companion object {
        /** 이 길이부터는 부분 문자열로 센다. 한 글자 표면형(ㅇ, 웅)은 낱말일 때만 센다. */
        private const val MIN_SUBSTRING_LENGTH = 2
        private const val TOKEN_START = """(?<![\p{L}\p{N}])"""
        private const val TOKEN_END = """(?![\p{L}\p{N}])"""
    }
}
