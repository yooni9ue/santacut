package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * 고유명사 사전(rules.json의 proper_nouns). JSON에서는 배열 하나로 읽는다.
 */
class MaskTerms @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(
    private val values: List<MaskTerm>,
) {
    @JsonValue
    fun toList(): List<MaskTerm> = values.toList()

    /** 묶음마다 비었거나 빈 낱말이 있으면 위반이다. */
    fun violations(): List<String> = values.filter { term -> term.words.isEmpty() || term.words.any(String::isBlank) }
        .map { term -> "rules.proper_nouns.${term.label}: 낱말이 비었다" }

    /** 낱말 경계 정규식으로 미리 바꿔 둔 사전. [particles]는 낱말 뒤에 붙어도 되는 조사 정규식이다. */
    fun compile(particles: String): MaskPatterns = MaskPatterns(values.map { term -> MaskPattern(term.regex(particles), term.label) })
}
