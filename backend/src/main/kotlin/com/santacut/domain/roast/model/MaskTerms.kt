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

    /** 낱말 앞 경계 정규식으로 미리 바꿔 둔 사전. 사전 낱말로 시작하는 흔한 말 [exceptions]는 건너뛴다. */
    fun compile(exceptions: List<String>): MaskPatterns = MaskPatterns(values.map { term -> MaskPattern(term.regex(), term.label, exceptions) })
}
