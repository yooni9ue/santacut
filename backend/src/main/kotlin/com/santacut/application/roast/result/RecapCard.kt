package com.santacut.application.roast.result

import com.santacut.domain.share.model.RecapDeckCard

/**
 * 리캡 덱 안 카드 한 장. 종류([kind])마다 구현이 그 카드의 값을 쥔다. 덱 순서([index])는 [RecapCards]가 매긴다.
 * 로그인 사용자 리캡 응답과 공유 링크의 카드 스냅숏이 같은 카드 목록에서 나온다.
 */
sealed interface RecapCard {
    val index: Int
    val kind: RecapCardKind

    /** [index]번 자리로 옮긴 같은 카드. */
    fun at(index: Int): RecapCard

    /** 공유 링크에 저장할 카드 값. 모든 카드에 상단 바 [top]과 총점 표시 [scoreText]를 싣는다. */
    fun toDeckCard(top: RecapTopResult, scoreText: String): RecapDeckCard
}
