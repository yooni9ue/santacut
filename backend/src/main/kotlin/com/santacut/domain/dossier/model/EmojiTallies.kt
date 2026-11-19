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

        /** 본인이 보낸 이모지 [emojis]를 세어 많이 쓴 [LIMIT]개를 남긴다. 같으면 먼저 나온 이모지가 앞이다. */
        fun of(emojis: List<String>): EmojiTallies = EmojiTallies(
            emojis.groupingBy { emoji -> emoji }.eachCount()
                .map { (emoji, count) -> EmojiTally(emoji, count) }
                .sortedByDescending { tally -> tally.count }
                .take(LIMIT),
        )
    }
}
