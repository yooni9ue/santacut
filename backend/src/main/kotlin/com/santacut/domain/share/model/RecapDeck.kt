package com.santacut.domain.share.model

/**
 * 공유 링크를 만든 순간 고정한 리캡 카드 덱. `shares.recap_json`에 JSON으로 저장한다.
 * 공유해도 되는 카드 값(AI 문장과 서버 숫자)만 담는다. 원문 대화, 상대 말, 참가자 목록, 프롬프트, 조서 문장은 담지 않는다.
 *
 * @property version 저장 형식 판. 읽을 때 [VERSION]과 다르면 카드 없이 요약 화면으로 보여 준다.
 * @property skin 카드 스킨(GIFT, COAL)
 * @property cards 카드 목록. 로그인 사용자 리캡 응답과 같은 순서다.
 */
data class RecapDeck(
    val version: Int,
    val skin: String,
    val cards: RecapDeckCards,
) {
    /** 지금 코드가 읽을 수 있는 판인지. */
    fun hasCurrentVersion(): Boolean = version == VERSION

    companion object {
        /** 지금 저장 형식 판. 카드 필드를 바꾸면 올린다. */
        const val VERSION = 1

        fun of(skin: String, cards: RecapDeckCards): RecapDeck = RecapDeck(VERSION, skin, cards)
    }
}
