package com.santacut.domain.dossier.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * 많이 쓴 이모지 목록. 순서가 순위다. JSON에서는 배열 하나로 읽고 쓴다.
 */
class EmojiTallies @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(
    private val values: List<EmojiTally>,
) {
    @JsonValue
    fun toList(): List<EmojiTally> = values.toList()

    /** 가장 많이 쓴 이모지. 없으면 null이다. */
    fun top(): EmojiTally? = values.getOrNull(0)

    companion object {
        const val LIMIT = 3
    }
}
