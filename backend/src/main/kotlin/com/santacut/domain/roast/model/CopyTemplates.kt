package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * 대체 라벨이나 대체 한 줄 후보 목록(roast/fallback.json). [PHRASE]가 든 후보는 칩의 입버릇 표기를 넣어 쓰는 틀이고, 없는 후보는 그대로 쓰는 고정 문장이다.
 * 틀을 채울 수 있으면 틀을, 채울 수 없으면(입버릇이 없거나 넣고 나서 너무 길면) 고정 문장을 쓴다. JSON에서는 배열 하나로 읽고 쓴다.
 */
class CopyTemplates @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(
    private val values: List<String>,
) {
    @JsonValue
    fun toList(): List<String> = values.toList()

    /** 고정 문장이 하나도 없는지. 틀을 채우지 못할 때 쓸 문장이 있어야 한다. */
    fun lacksPlain(): Boolean = values.none { template -> PHRASE !in template }

    /**
     * [index]번째(0부터, 돌려 씀) 문장. 입버릇 [phrase]로 채운 틀이 [maxLength]자 안이면 그 틀들 가운데서, 아니면 고정 문장 가운데서 고른다.
     * 틀은 입버릇을 작은따옴표로 감싸 든다. 칩에 찍힌 표기를 그대로 옮긴 것이라 말투·인용 검사에서 문장 낱말로 치지 않는다.
     */
    fun pick(index: Int, phrase: String?, maxLength: Int): String {
        val filled = phrase?.let { value ->
            values.filter { template -> PHRASE in template }.map { template -> template.replace(PHRASE, value) }
                .filter { text -> text.codePointCount(0, text.length) <= maxLength }
        }.orEmpty()
        val pool = filled.ifEmpty { values.filterNot { template -> PHRASE in template } }
        return pool[index.mod(pool.size)]
    }

    companion object {
        /** 입버릇 표기를 넣을 자리. */
        const val PHRASE = "{phrase}"
    }
}
