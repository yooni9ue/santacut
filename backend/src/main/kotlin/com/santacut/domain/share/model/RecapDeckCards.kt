package com.santacut.domain.share.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

/**
 * 공유 링크에 저장한 리캡 카드 목록(2판). 저장한 순서가 곧 화면 순서다. JSON에서는 [RecapDeckCard] 배열 하나로 저장한다.
 */
data class RecapDeckCards @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(
    @field:JsonValue private val values: List<RecapDeckCard>,
) {
    /** 저장한 순서대로 하나씩 [convert]로 바꾼다. 화면에 그릴 값을 만들 때 쓴다. */
    fun <T> convertEach(convert: (RecapDeckCard) -> T): List<T> = values.map(convert)

    fun size(): Int = values.size

    companion object {
        private const val KIND = "kind"

        /** 카드 배열 [cards]를 읽는다. 2판 종류([RecapDeckKind])가 아닌 카드는 건너뛴다. */
        fun from(cards: JsonNode, objectMapper: ObjectMapper): RecapDeckCards = RecapDeckCards(
            cards.values().filter { card -> RecapDeckKind.isKnown(card.path(KIND).asString()) }
                .map { card -> objectMapper.treeToValue(card, RecapDeckCard::class.java) },
        )
    }
}
