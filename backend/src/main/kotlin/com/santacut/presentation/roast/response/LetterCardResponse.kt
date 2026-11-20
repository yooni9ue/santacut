package com.santacut.presentation.roast.response

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "산타 쪽지 카드(LETTER)")
data class LetterCardResponse(
    @field:Schema(description = "카드 순서(0부터)", example = "4")
    override val index: Int,
    @field:Schema(description = "카드 종류", allowableValues = ["LETTER"])
    override val kind: String,
    @field:Schema(description = "카드 가로 픽셀", example = "1080")
    override val width: Int,
    @field:Schema(description = "카드 세로 픽셀", example = "1920")
    override val height: Int,
    @field:JsonProperty("score_text")
    @field:Schema(description = "모든 카드 상단에 찍는 총점 표시", example = "73.73")
    override val scoreText: String,
    @field:Schema(description = "산타 쪽지(60~120자, 놀림 없음)")
    val letter: String,
) : RecapCardResponse {
    companion object {
        private const val KIND = "LETTER"

        fun of(index: Int, scoreText: String, letter: String): LetterCardResponse =
            LetterCardResponse(index, KIND, RecapCardResponse.WIDTH, RecapCardResponse.HEIGHT, scoreText, letter)
    }
}
