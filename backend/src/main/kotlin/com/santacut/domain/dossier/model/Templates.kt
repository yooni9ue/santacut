package com.santacut.domain.dossier.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * 같은 자리에 쓸 문구 템플릿 후보(copy.json의 문자열 배열). 고르기는 결과 ID 시드로 결정적이다.
 */
class Templates @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(
    private val values: List<String>,
) {
    @JsonValue
    fun toList(): List<String> = values.toList()

    /**
     * 규칙 위반 설명. 후보가 없거나, 아라비아 숫자를 직접 썼거나, 슬롯 문법이 틀렸거나, 금지·미등록 슬롯을 쓰면 위반이다.
     * [path]는 위반 위치를 알리는 copy.json 경로, [registered]는 copy.json `slots`에 등록한 슬롯 이름이다.
     */
    fun violations(path: String, registered: Set<String>): List<String> =
        listOfNotNull("$path: 문구가 없다".takeIf { values.isEmpty() }) + values.flatMap { text -> violationsOf(path, text, registered) }

    /** [slots]로 다 채울 수 있는 후보 중 [seed]로 하나를 골라 채운다. 채울 수 있는 후보가 없으면 null이다. */
    fun pick(seed: TemplateSeed, slots: Slots): String? {
        val fillable = values.filter(slots::canFill)
        if (fillable.isEmpty()) return null
        return slots.fill(fillable[seed.indexFor(fillable.size)])
    }

    /** 채울 수 있는 후보 중 [seed]로 정한 자리부터 서로 다른 문구를 [limit]개까지 골라 줄로 만든다. */
    fun pickLines(seed: TemplateSeed, slots: Slots, limit: Int, shareable: Boolean): List<LineDraft> {
        val fillable = values.filter(slots::canFill)
        if (fillable.isEmpty()) return emptyList()
        val start = seed.indexFor(fillable.size)
        return List(minOf(limit, fillable.size)) { offset -> LineDraft(slots.fill(fillable[(start + offset) % fillable.size]), shareable) }
    }

    private fun violationsOf(path: String, text: String, registered: Set<String>): List<String> {
        val names = Slots.namesIn(text)
        return listOfNotNull("$path: 아라비아 숫자는 슬롯으로만 쓴다: $text".takeIf { DIGIT.containsMatchIn(text) }) +
            BRACE.findAll(text).map { match -> match.value }.filterNot(Slots.SLOT::matches).map { slot -> "$path: 슬롯 문법이 틀렸다: $slot" } +
            names.filter { name -> name in Slots.FORBIDDEN }.map { name -> "$path: 금지 슬롯 {$name}" } +
            names.filterNot { name -> name in registered || name in Slots.FORBIDDEN }.map { name -> "$path: 미등록 슬롯 {$name}" }
    }

    companion object {
        private val DIGIT = Regex("[0-9]")
        private val BRACE = Regex("\\{[^{}]*\\}|[{}]")
    }
}
