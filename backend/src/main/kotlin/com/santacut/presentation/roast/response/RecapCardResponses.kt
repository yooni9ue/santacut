package com.santacut.presentation.roast.response

import com.fasterxml.jackson.annotation.JsonValue
import com.santacut.application.roast.result.RecapCard
import com.santacut.application.roast.result.RecapResult
import com.santacut.application.roast.result.RecapCardKind
import com.santacut.presentation.ranking.response.ArchetypeResponse
import com.santacut.presentation.ranking.response.DimensionBarResponses

/**
 * 리캡 카드 덱. 순서는 [RecapResult.cards]([com.santacut.application.roast.result.RecapCards])를 그대로 따른다: 판정, 팩폭 세 장, 쪽지, 줄거리(있을 때만), 유형, 항목 막대.
 * JSON에서는 카드 배열 하나로 나간다.
 */
class RecapCardResponses(@field:JsonValue private val values: List<RecapCardResponse>) {
    companion object {
        fun from(result: RecapResult): RecapCardResponses = RecapCardResponses(result.cards().values.map { card -> responseOf(card, result) })

        private fun responseOf(card: RecapCard, result: RecapResult): RecapCardResponse {
            val score = result.scoreText
            return when (card.kind) {
                RecapCardKind.VERDICT -> VerdictCardResponse.from(card.index, result)
                RecapCardKind.ROAST -> RoastCardResponse.from(card.index, score, requireNotNull(card.pair))
                RecapCardKind.LETTER -> LetterCardResponse.of(card.index, score, result.letter)
                RecapCardKind.STORY -> StoryCardResponse.of(card.index, score, StoryActResponses.from(result.story))
                RecapCardKind.ARCHETYPE -> ArchetypeCardResponse.of(card.index, score, ArchetypeResponse(result.archetypeCode, result.archetypeName))
                RecapCardKind.DIMENSIONS -> DimensionsCardResponse.of(card.index, score, DimensionBarResponses.from(result.dimensions))
            }
        }
    }
}
