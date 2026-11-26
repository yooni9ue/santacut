package com.santacut.domain.share.model

/**
 * 공유 링크에 저장한 리캡 카드 종류. 로그인 사용자 리캡 응답의 카드 `kind`와 같은 이름이고, 저장 JSON에도 이 이름으로 들어간다.
 * 덱 안 순서는 정하지 않는다. 순서는 공유 링크를 만들 때 애플리케이션 계층이 정해 [RecapDeck.cards]에 담는다.
 *
 * @property label 공개 페이지에 보일 카드 종류 이름
 */
enum class RecapDeckKind(val label: String) {
    VERDICT("판정"),
    ROAST("팩폭"),
    LETTER("산타 쪽지"),
    STORY("올해 줄거리"),
    ARCHETYPE("산타 유형"),
    DIMENSIONS("항목 점수"),
}
