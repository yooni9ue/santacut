package com.santacut.domain.share.model

import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

/**
 * 저장 형식 1판 리캡 카드 덱(`{"version":1,...}`). 읽기 전용이다. 새 링크는 [RecapDeck](2판)으로 저장하고,
 * 이미 저장한 1판 덱은 옛 카드 종류([RecapDeckV1Kind]: 판정, 팩폭, 쪽지, 줄거리, 유형, 항목 막대) 그대로 공개 페이지에 그린다.
 *
 * @property skin 카드 스킨(GIFT, COAL)
 * @property cards 카드 목록. 저장한 순서 그대로다.
 */
data class RecapDeckV1(val skin: String, val cards: RecapDeckV1Cards) {
    companion object {
        const val VERSION = 1
        private const val SKIN = "skin"
        private const val CARDS = "cards"

        /** 판을 확인한 뒤의 저장 JSON [root]를 읽는다. 모르는 종류의 카드는 덱 전체를 버리지 않고 그 카드만 건너뛴다. */
        fun from(root: JsonNode, objectMapper: ObjectMapper): RecapDeckV1 =
            RecapDeckV1(root.path(SKIN).asString(), RecapDeckV1Cards.from(root.path(CARDS), objectMapper))
    }
}
