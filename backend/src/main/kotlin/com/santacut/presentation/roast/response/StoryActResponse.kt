package com.santacut.presentation.roast.response

import com.santacut.application.roast.result.RecapStoryResult
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "줄거리 카드의 막 하나")
data class StoryActResponse(
    @field:Schema(description = "막 번호(1~3)", example = "1")
    val act: Int,
    @field:Schema(description = "막의 달 범위", example = "1~4월")
    val months: String,
    @field:Schema(description = "그 막의 한 줄")
    val line: String,
) {
    companion object {
        fun from(result: RecapStoryResult): StoryActResponse = StoryActResponse(result.act, result.months, result.line)
    }
}
