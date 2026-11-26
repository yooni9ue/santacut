package com.santacut.application.roast.result

import com.santacut.domain.roast.model.RoastStoryLines
import com.santacut.domain.share.model.RecapDeckAct
import com.santacut.domain.share.model.RecapDeckActs

/** 리캡 줄거리 카드의 막 목록. 저장한 줄거리가 없으면 빈 목록이고 카드도 빠진다. */
data class RecapStoryResults(val values: List<RecapStoryResult>) {
    fun isEmpty(): Boolean = values.isEmpty()

    /** 공유 링크에 저장할 막 목록. */
    fun toDeckActs(): RecapDeckActs = RecapDeckActs(values.map { story -> RecapDeckAct(story.act, story.months, story.line) })

    companion object {
        fun from(story: RoastStoryLines): RecapStoryResults = RecapStoryResults(story.toList().map(RecapStoryResult::from))
    }
}
