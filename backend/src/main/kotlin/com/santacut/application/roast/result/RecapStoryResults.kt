package com.santacut.application.roast.result

import com.santacut.domain.roast.model.RoastStoryLines

/** 리캡 줄거리 카드의 막 목록. 저장한 줄거리가 없으면 빈 목록이고 카드도 빠진다. */
data class RecapStoryResults(val values: List<RecapStoryResult>) {
    fun isEmpty(): Boolean = values.isEmpty()

    companion object {
        fun from(story: RoastStoryLines): RecapStoryResults = RecapStoryResults(story.toList().map(RecapStoryResult::from))
    }
}
