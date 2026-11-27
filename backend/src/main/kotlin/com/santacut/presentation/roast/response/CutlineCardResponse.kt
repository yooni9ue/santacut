package com.santacut.presentation.roast.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.santacut.application.roast.result.RecapCutlineCard
import com.santacut.application.roast.result.RecapTopResult
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "컷 표 카드(CUTLINE). 여섯 구간 세로 표에서 내 칸만 불을 켜고 구간 고정 문구와 순위 칩을 싣습니다. 값은 모두 서버 것입니다.")
data class CutlineCardResponse(
    @field:Schema(description = "카드 순서(0부터)", example = "1")
    override val index: Int,
    @field:Schema(description = "카드 종류", allowableValues = ["CUTLINE"])
    override val kind: String,
    @field:Schema(description = "카드 가로 픽셀", example = "1080")
    override val width: Int,
    @field:Schema(description = "카드 세로 픽셀", example = "1920")
    override val height: Int,
    @field:JsonProperty("score_text")
    @field:Schema(description = "총점 표시. 표에 점수 점을 찍는 데 씁니다.", example = "73.73")
    override val scoreText: String,
    @field:Schema(description = "상단 바와 큰 칩의 전국 순위. 순위 반영 전이면 null입니다.", nullable = true)
    override val rank: Int?,
    @field:JsonProperty("top_percent")
    @field:Schema(description = "상단 바와 큰 칩의 상위 비율(%). 순위가 없거나 참가자가 100명 아래면 null이고, 그때 상단 바는 순위(전국 N등)만 씁니다.", nullable = true)
    override val topPercent: Int?,
    @field:Schema(description = "그 순위 계산의 참가자 수. 순위가 없으면 null입니다.", nullable = true)
    val total: Long?,
    @field:Schema(description = "내 구간 이름", example = "굴뚝에 걸쳐서 통과")
    val grade: String,
    @field:JsonProperty("grade_code")
    @field:Schema(description = "내 구간 코드", allowableValues = ["PREMIUM", "GIFT_DEPARTED", "SCRAPED_THROUGH", "ONE_STEP_SHORT", "OFF_LIST", "COAL"])
    val gradeCode: String,
    @field:JsonProperty("band_detail")
    @field:Schema(description = "구간 고정 문구(서버 상수)", example = "바지 걸렸는데 통과")
    val bandDetail: String,
    @field:JsonProperty("band_position")
    @field:Schema(description = "내 칸의 자리(0부터, 0이 가장 높은 구간)", example = "2")
    val bandPosition: Int,
    @field:ArraySchema(arraySchema = Schema(description = "여섯 칸(높은 구간부터)"), schema = Schema(implementation = BandResponse::class))
    val bands: BandResponses,
) : RecapCardResponse {
    companion object {
        private const val KIND = "CUTLINE"

        fun from(card: RecapCutlineCard, top: RecapTopResult, scoreText: String): CutlineCardResponse = CutlineCardResponse(
            card.index, KIND, RecapCardResponse.WIDTH, RecapCardResponse.HEIGHT, scoreText, top.rank, top.topPercent, top.total,
            card.grade, card.gradeCode, card.bandDetail, card.bandPosition, BandResponses.from(card.bands),
        )
    }
}
