package com.santacut.domain.share.model

/**
 * 공유 링크에 저장한 리캡 카드 종류(저장 형식 2판). 로그인 사용자 리캡 응답의 카드 `kind`와 같은 이름이고, 저장 JSON에도 이 이름으로 들어간다.
 * 덱 안 순서는 정하지 않는다. 순서는 공유 링크를 만들 때 애플리케이션 계층이 정해 [RecapDeck.cards]에 담는다.
 *
 * @property label 공개 페이지에 보일 카드 종류 이름
 */
enum class RecapDeckKind(val label: String) {
    VERDICT("판정"),
    CUTLINE("산타컷 표"),
    REASON("선물 받는 이유"),
    CUT("점수 깎인 말"),
    KEEP("살아남은 말"),
    CONCLUSION("올해의 결론"),
    ;

    companion object {
        /** 저장 JSON의 kind 글자가 2판 종류인지. */
        fun isKnown(name: String): Boolean = entries.any { kind -> kind.name == name }
    }
}
