package com.santacut.presentation.roast.response

import com.fasterxml.jackson.annotation.JsonValue
import com.santacut.application.roast.result.RecapResult
import com.santacut.presentation.ranking.response.ArchetypeResponse
import com.santacut.presentation.ranking.response.DimensionBarResponses

/**
 * 리캡 카드 덱. 순서는 고정이다: 판정, 팩폭 세 장, 쪽지, 줄거리(있을 때만), 유형, 항목 막대. index는 0부터 이 순서다.
 * JSON에서는 카드 배열 하나로 나간다.
 */
class RecapCardResponses(@field:JsonValue private val values: List<RecapCardResponse>) {
    companion object {
        fun from(result: RecapResult): RecapCardResponses {
            val score = result.scoreText
            val builders = listOf<(Int) -> RecapCardResponse> { index -> VerdictCardResponse.from(index, result) } +
                result.pairs.values.map { pair -> { index: Int -> RoastCardResponse.from(index, score, pair) } } +
                { index: Int -> LetterCardResponse.of(index, score, result.letter) } +
                listOfNotNull(
                    { index: Int -> StoryCardResponse.of(index, score, StoryActResponses.from(result.story)) }.takeUnless { result.story.isEmpty() },
                ) +
                { index: Int -> ArchetypeCardResponse.of(index, score, ArchetypeResponse(result.archetypeCode, result.archetypeName)) } +
                { index: Int -> DimensionsCardResponse.of(index, score, DimensionBarResponses.from(result.dimensions)) }
            return RecapCardResponses(builders.mapIndexed { index, build -> build(index) })
        }
    }
}
