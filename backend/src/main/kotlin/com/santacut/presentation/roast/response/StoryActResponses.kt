package com.santacut.presentation.roast.response

import com.fasterxml.jackson.annotation.JsonValue
import com.santacut.application.roast.result.RecapStoryResults

/** 줄거리 카드의 막 목록. JSON에서는 [StoryActResponse] 배열 하나로 나간다. */
class StoryActResponses(@field:JsonValue private val values: List<StoryActResponse>) {
    companion object {
        fun from(results: RecapStoryResults): StoryActResponses = StoryActResponses(results.values.map(StoryActResponse::from))
    }
}
