package com.santacut.domain.share.service

import com.santacut.domain.share.model.RecapDeck
import com.santacut.domain.share.model.RecapDeckV1
import com.santacut.infrastructure.share.render.ShareDeck
import tools.jackson.databind.ObjectMapper

/**
 * 저장한 리캡 카드 덱 JSON(`shares.recap_json`)을 판에 맞는 읽기로 보내 공개 페이지에 그릴 값으로 바꾼다.
 * JSON을 트리로 읽어 version부터 보고, 1판은 [RecapDeckV1]과 [ShareDeckV1Converter], 2판은 [RecapDeck]과 [ShareDeckConverter]로 읽는다.
 * 모르는 판이면 null이라 요약 화면으로 보인다. 덱 안의 모르는 종류 카드는 덱 전체를 버리지 않고 그 카드만 건너뛴다.
 */
object ShareDeckReader {
    private const val VERSION = "version"

    /** [json]을 읽는다. 모르는 판이면 null이다. JSON이 깨졌으면 [tools.jackson.core.JacksonException]을 던진다. */
    fun read(json: String, objectMapper: ObjectMapper): ShareDeck? {
        val root = objectMapper.readTree(json)
        return when (root.path(VERSION).asInt()) {
            RecapDeckV1.VERSION -> ShareDeckV1Converter.of(RecapDeckV1.from(root, objectMapper))
            RecapDeck.VERSION -> ShareDeckConverter.of(RecapDeck.from(root, objectMapper))
            else -> null
        }
    }
}
