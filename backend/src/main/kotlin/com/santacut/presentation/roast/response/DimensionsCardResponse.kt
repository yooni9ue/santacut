package com.santacut.presentation.roast.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.santacut.presentation.ranking.response.DimensionBarResponse
import com.santacut.presentation.ranking.response.DimensionBarResponses
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "항목 막대 카드(DIMENSIONS). 최신 평가의 4항목 막대입니다.")
data class DimensionsCardResponse(
    @field:Schema(description = "카드 순서(0부터)", example = "7")
    override val index: Int,
    @field:Schema(description = "카드 종류", allowableValues = ["DIMENSIONS"])
    override val kind: String,
    @field:Schema(description = "카드 가로 픽셀", example = "1080")
    override val width: Int,
    @field:Schema(description = "카드 세로 픽셀", example = "1920")
    override val height: Int,
    @field:JsonProperty("score_text")
    @field:Schema(description = "모든 카드 상단에 찍는 총점 표시", example = "73.73")
    override val scoreText: String,
    @field:ArraySchema(
        arraySchema = Schema(description = "항목 막대 4개(공감·응원·존중·협력 순서)"),
        schema = Schema(implementation = DimensionBarResponse::class),
    )
    val bars: DimensionBarResponses,
) : RecapCardResponse {
    companion object {
        private const val KIND = "DIMENSIONS"

        fun of(index: Int, scoreText: String, bars: DimensionBarResponses): DimensionsCardResponse =
            DimensionsCardResponse(index, KIND, RecapCardResponse.WIDTH, RecapCardResponse.HEIGHT, scoreText, bars)
    }
}
