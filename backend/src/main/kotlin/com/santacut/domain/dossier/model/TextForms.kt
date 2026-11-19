package com.santacut.domain.dossier.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * 본인 메시지에서 셀 표면형 목록. 두 글자 이상은 부분 문자열로 세고, 한 글자는 앞뒤에 글자·숫자가 붙지 않은 낱말일 때만 센다.
 * 한 글자 표면형이 한글 음절이면 바로 붙은 ㅋ·ㅎ·ㅠ·ㅜ는 낱말 경계로 본다('헐ㅋㅋ', '왜ㅠㅠ'의 헐·왜). 'ㅇㅋ'의 ㅇ처럼 자모 표면형은 그대로 막는다.
 * 겹쳐 나온 자리는 한 번만 센다. JSON에서는 문자열 배열 하나로 읽는다.
 */
class TextForms @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(
    private val values: List<String>,
) {
    @JsonValue
    fun toList(): List<String> = values.toList()

    /** 표면형이 하나도 없거나 빈 표면형이 섞여 있는지. */
    fun isMalformed(): Boolean = values.isEmpty() || values.any { form -> form.isBlank() }

    /** [text] 안에 나온 모든 표면형의 횟수 합. [wholeWord]면 길이와 상관없이 낱말 경계에서만 센다. */
    fun countIn(text: String, wholeWord: Boolean = false): Int = values.sumOf { form ->
        if (wholeWord || form.length < MIN_SUBSTRING_LENGTH) tokenCount(text, form, 0, 0) else substringCount(text, form, 0, 0)
    }

    /** [text]에 표면형이 하나라도 나왔는지. */
    fun appearsIn(text: String): Boolean = countIn(text) > 0

    private tailrec fun tokenCount(text: String, form: String, from: Int, found: Int): Int {
        val index = text.indexOf(form, from)
        if (index < 0) return found
        val end = index + form.length
        val isToken = !joins(text.getOrNull(index - 1), form) && !joins(text.getOrNull(end), form)
        return if (isToken) tokenCount(text, form, end, found + 1) else tokenCount(text, form, index + 1, found)
    }

    private tailrec fun substringCount(text: String, form: String, from: Int, found: Int): Int {
        val index = text.indexOf(form, from)
        if (index < 0) return found
        return substringCount(text, form, index + form.length, found + 1)
    }

    /** 한 글자 [form] 옆의 [neighbor]가 그 글자와 이어져 낱말이 되는지. */
    private fun joins(neighbor: Char?, form: String): Boolean {
        if (neighbor == null || !neighbor.isLetterOrDigit()) return false
        return !(neighbor in LAUGH_OR_CRY && form[0].code in SYLLABLES)
    }

    companion object {
        /** 이 길이부터는 부분 문자열로 센다. 한 글자 표면형(ㅇ, 웅)은 낱말일 때만 센다. */
        private const val MIN_SUBSTRING_LENGTH = 2
        private const val LAUGH_OR_CRY = "ㅋㅎㅠㅜ"
        private val SYLLABLES = 0xAC00..0xD7A3
    }
}
