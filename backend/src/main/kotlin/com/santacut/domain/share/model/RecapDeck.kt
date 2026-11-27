package com.santacut.domain.share.model

import com.fasterxml.jackson.annotation.JsonIgnore
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

/**
 * 공유 링크를 만든 순간 고정한 리캡 카드 덱(저장 형식 2판). `shares.recap_json`에 `{"version":2,...}` JSON으로 저장한다.
 * 공유해도 되는 카드 값(AI 문장, 서버 숫자, 이름을 가린 본인 말풍선)만 담는다. 원문 대화, 상대 말, 참가자 목록, 프롬프트, 조서 문장은 담지 않는다.
 * 1판으로 저장한 옛 덱은 [RecapDeckV1]이 읽는다.
 *
 * @property version 저장 형식 판([VERSION])
 * @property skin 카드 스킨(GIFT, COAL)
 * @property cards 카드 목록. 로그인 사용자 리캡 응답과 같은 순서다.
 */
data class RecapDeck(
    val version: Int,
    val skin: String,
    val cards: RecapDeckCards,
) {
    @JsonIgnore
    fun isGift(): Boolean = skin == GIFT_SKIN

    companion object {
        /** 지금 저장 형식 판. 카드 필드를 바꾸면 올리고, 옛 판 읽기는 따로 남긴다. */
        const val VERSION = 2
        private const val GIFT_SKIN = "GIFT"
        private const val SKIN = "skin"
        private const val CARDS = "cards"

        fun of(skin: String, cards: RecapDeckCards): RecapDeck = RecapDeck(VERSION, skin, cards)

        /** 판을 확인한 뒤의 저장 JSON [root]를 읽는다. 모르는 종류의 카드는 덱 전체를 버리지 않고 그 카드만 건너뛴다. */
        fun from(root: JsonNode, objectMapper: ObjectMapper): RecapDeck =
            RecapDeck(VERSION, root.path(SKIN).asString(), RecapDeckCards.from(root.path(CARDS), objectMapper))
    }
}
