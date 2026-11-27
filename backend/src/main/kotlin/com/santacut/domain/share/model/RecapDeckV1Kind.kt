package com.santacut.domain.share.model

/**
 * 저장 형식 1판 덱의 카드 종류. 읽기 전용이다. 이미 저장한 1판 링크를 그대로 보여 주려고 남겨 두며, 새 덱은 [RecapDeckKind]를 쓴다.
 *
 * @property label 공개 페이지에 보일 카드 종류 이름
 */
enum class RecapDeckV1Kind(val label: String) {
    VERDICT("판정"),
    ROAST("팩폭"),
    LETTER("산타 쪽지"),
    STORY("올해 줄거리"),
    ARCHETYPE("산타 유형"),
    DIMENSIONS("항목 점수"),
    ;

    companion object {
        /** 저장 JSON의 kind 글자가 1판 종류인지. */
        fun isKnown(name: String): Boolean = entries.any { kind -> kind.name == name }
    }
}
