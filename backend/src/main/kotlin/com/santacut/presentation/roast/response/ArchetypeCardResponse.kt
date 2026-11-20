package com.santacut.presentation.roast.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.santacut.presentation.ranking.response.ArchetypeResponse
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "산타 유형 카드(ARCHETYPE). 결과 화면과 같은 유형 코드와 이름입니다.")
data class ArchetypeCardResponse(
    @field:Schema(description = "카드 순서(0부터)", example = "6")
    override val index: Int,
    @field:Schema(description = "카드 종류", allowableValues = ["ARCHETYPE"])
    override val kind: String,
    @field:Schema(description = "카드 가로 픽셀", example = "1080")
    override val width: Int,
    @field:Schema(description = "카드 세로 픽셀", example = "1920")
    override val height: Int,
    @field:JsonProperty("score_text")
    @field:Schema(description = "모든 카드 상단에 찍는 총점 표시", example = "73.73")
    override val scoreText: String,
    @field:Schema(description = "재미용 산타 유형")
    val archetype: ArchetypeResponse,
) : RecapCardResponse {
    companion object {
        private const val KIND = "ARCHETYPE"

        fun of(index: Int, scoreText: String, archetype: ArchetypeResponse): ArchetypeCardResponse =
            ArchetypeCardResponse(index, KIND, RecapCardResponse.WIDTH, RecapCardResponse.HEIGHT, scoreText, archetype)
    }
}
