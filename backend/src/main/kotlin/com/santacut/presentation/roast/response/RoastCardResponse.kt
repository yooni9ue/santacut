package com.santacut.presentation.roast.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.santacut.application.roast.result.RecapPairResult
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "팩폭 카드(ROAST). 서버 숫자 칩, 버릇 놀림, 같은 버릇 뒤집기를 싣습니다. 덱에 세 장 들어갑니다.")
data class RoastCardResponse(
    @field:Schema(description = "카드 순서(0부터)", example = "1")
    override val index: Int,
    @field:Schema(description = "카드 종류", allowableValues = ["ROAST"])
    override val kind: String,
    @field:Schema(description = "카드 가로 픽셀", example = "1080")
    override val width: Int,
    @field:Schema(description = "카드 세로 픽셀", example = "1920")
    override val height: Int,
    @field:JsonProperty("score_text")
    @field:Schema(description = "모든 카드 상단에 찍는 총점 표시", example = "73.73")
    override val scoreText: String,
    @field:Schema(description = "서버 숫자 칩. 사실 없이 쓴 대체 문장이면 null입니다.", nullable = true)
    val chip: ChipResponse?,
    @field:Schema(description = "버릇 놀림(숫자 없음)")
    val roast: String,
    @field:Schema(description = "같은 버릇 뒤집기. 문장이 '근데 '로 시작하니 화면은 '근데' 라벨을 따로 붙이지 않습니다. 이 기능 앞 결과에 저장된 문장은 '근데' 없이 시작할 수 있습니다.")
    val flip: String,
) : RecapCardResponse {
    companion object {
        private const val KIND = "ROAST"

        fun from(index: Int, scoreText: String, pair: RecapPairResult): RoastCardResponse = RoastCardResponse(
            index, KIND, RecapCardResponse.WIDTH, RecapCardResponse.HEIGHT, scoreText, chipOf(pair), pair.roast, pair.flip,
        )

        private fun chipOf(pair: RecapPairResult): ChipResponse? {
            val label = pair.chipLabel ?: return null
            val value = pair.chipValue ?: return null
            return ChipResponse(label, value)
        }
    }
}
