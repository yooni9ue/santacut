package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/** 작가가 써도 되는 유행어 목록(roast/slang.json의 allowed). JSON에서는 배열 하나로 읽고 쓴다. */
class SlangWords @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(
    private val values: List<SlangWord>,
) {
    @JsonValue
    fun toList(): List<SlangWord> = values.toList()

    /** 작가 입력용 표기 → 뜻. 예문은 문장에 그대로 새지 않게 보내지 않는다. */
    fun hints(): Map<String, String> = values.associate { word -> word.word to word.meaning }

    /** 사람용 예문. 작가에게 보내지 않지만 베끼면 안 되는 예시 문장([ExampleLines])에 넣는다. */
    fun examples(): List<String> = values.map(SlangWord::example)

    /** 세는 줄기. 긴 것부터라 '개야르'가 '야르'로 한 번 더 세지지 않는다. */
    fun stems(): List<String> = values.map(SlangWord::stem).sortedByDescending(String::length)

    /** 표기·줄기·뜻이 빈 항목, 겹친 표기, 금지 말([banned])을 품은 항목. */
    fun violations(banned: List<String>): List<String> =
        values.filter { word -> word.word.isBlank() || word.stem.isBlank() || word.meaning.isBlank() || word.stem !in word.word }
            .map { word -> "slang.allowed.${word.word}: word·stem·meaning이 비었거나 stem이 word 안에 없다" } +
            values.groupBy(SlangWord::word).filterValues { same -> same.size > 1 }.keys.map { word -> "slang.allowed: $word 중복" } +
            values.filter { word -> banned.any { ban -> ban in word.word } }.map { word -> "slang.allowed.${word.word}: 금지 말을 품었다" }

    companion object {
        val EMPTY = SlangWords(emptyList())
    }
}
