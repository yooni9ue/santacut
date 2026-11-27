package com.santacut.domain.share.model

import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

/** 저장 형식 1판 덱의 카드 목록. 읽기 전용이고, 저장한 순서가 곧 화면 순서다. */
data class RecapDeckV1Cards(private val values: List<RecapDeckV1Card>) {
    /** 저장한 순서대로 하나씩 [convert]로 바꾼다. 화면에 그릴 값을 만들 때 쓴다. */
    fun <T> convertEach(convert: (RecapDeckV1Card) -> T): List<T> = values.map(convert)

    fun size(): Int = values.size

    companion object {
        private const val KIND = "kind"

        /** 카드 배열 [cards]를 읽는다. 1판 종류([RecapDeckV1Kind])가 아닌 카드는 건너뛴다. */
        fun from(cards: JsonNode, objectMapper: ObjectMapper): RecapDeckV1Cards = RecapDeckV1Cards(
            cards.values().filter { card -> RecapDeckV1Kind.isKnown(card.path(KIND).asString()) }
                .map { card -> objectMapper.treeToValue(card, RecapDeckV1Card::class.java) },
        )
    }
}
