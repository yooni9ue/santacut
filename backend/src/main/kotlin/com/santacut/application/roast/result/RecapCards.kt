package com.santacut.application.roast.result

import com.santacut.domain.share.model.RecapDeckCards

/**
 * 리캡 카드 덱의 순서. 카드 종류 순서를 정하는 유일한 곳이다: 판정, 팩폭 세 장, 쪽지, 줄거리(있을 때만), 유형, 항목 막대.
 * 로그인 사용자 리캡 응답(`RecapCardResponses`)과 공유 링크 카드 스냅숏([toDeckCards])이 모두 이 목록을 따른다.
 */
data class RecapCards(val values: List<RecapCard>) {
    /** 공유 링크에 저장할 카드 목록. 순서와 index는 그대로다. */
    fun toDeckCards(result: RecapResult): RecapDeckCards = RecapDeckCards(values.map { card -> card.toDeckCard(result) })

    companion object {
        /** index를 매기기 전 자리표시 값. [of]가 순서대로 0부터 다시 매긴다. */
        private const val UNNUMBERED = -1

        fun of(result: RecapResult): RecapCards {
            val unnumbered = listOf(RecapCard(UNNUMBERED, RecapCardKind.VERDICT)) +
                result.pairs.values.map { pair -> RecapCard(UNNUMBERED, RecapCardKind.ROAST, pair) } +
                RecapCard(UNNUMBERED, RecapCardKind.LETTER) +
                listOfNotNull(RecapCard(UNNUMBERED, RecapCardKind.STORY).takeUnless { result.story.isEmpty() }) +
                RecapCard(UNNUMBERED, RecapCardKind.ARCHETYPE) +
                RecapCard(UNNUMBERED, RecapCardKind.DIMENSIONS)
            return RecapCards(unnumbered.mapIndexed { index, card -> card.copy(index = index) })
        }
    }
}
