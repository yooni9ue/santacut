package com.santacut.presentation.roast.response

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "세 막 줄거리 카드(STORY). 저장한 줄거리가 있을 때만 덱에 들어갑니다.")
data class StoryCardResponse(
    @field:Schema(description = "카드 순서(0부터)", example = "5")
    override val index: Int,
    @field:Schema(description = "카드 종류", allowableValues = ["STORY"])
    override val kind: String,
    @field:Schema(description = "카드 가로 픽셀", example = "1080")
    override val width: Int,
    @field:Schema(description = "카드 세로 픽셀", example = "1920")
    override val height: Int,
    @field:JsonProperty("score_text")
    @field:Schema(description = "모든 카드 상단에 찍는 총점 표시", example = "73.73")
    override val scoreText: String,
    @field:ArraySchema(
        arraySchema = Schema(description = "막 목록(1막 1~4월, 2막 5~8월, 3막 9~12월 순서)"),
        schema = Schema(implementation = StoryActResponse::class),
    )
    val acts: StoryActResponses,
) : RecapCardResponse {
    companion object {
        private const val KIND = "STORY"

        fun of(index: Int, scoreText: String, acts: StoryActResponses): StoryCardResponse =
            StoryCardResponse(index, KIND, RecapCardResponse.WIDTH, RecapCardResponse.HEIGHT, scoreText, acts)
    }
}
