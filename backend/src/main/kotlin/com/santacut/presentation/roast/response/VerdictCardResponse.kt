package com.santacut.presentation.roast.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.santacut.application.roast.result.RecapTopResult
import com.santacut.application.roast.result.RecapVerdictCard
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(description = "판정 카드(VERDICT). 큰 글자(착한 아이·나쁜 아이), 점수·컷 거리 칩, 선물 도착일 칩, AI 한 줄을 싣습니다.")
data class VerdictCardResponse(
    @field:Schema(description = "카드 순서(0부터)", example = "0")
    override val index: Int,
    @field:Schema(description = "카드 종류", allowableValues = ["VERDICT"])
    override val kind: String,
    @field:Schema(description = "카드 가로 픽셀", example = "1080")
    override val width: Int,
    @field:Schema(description = "카드 세로 픽셀", example = "1920")
    override val height: Int,
    @field:JsonProperty("score_text")
    @field:Schema(description = "총점 표시", example = "73.73")
    override val scoreText: String,
    @field:Schema(description = "상단 바의 전국 순위. 순위 계산이 끝나면 늘 있고, 최신 결과가 아직 순위에 반영되지 않았으면 null입니다.", nullable = true)
    override val rank: Int?,
    @field:JsonProperty("top_percent")
    @field:Schema(description = "상단 바의 상위 비율(%, 1~100). 순위가 없거나 참가자가 100명 아래면 null이고, 그때 상단 바는 순위(전국 N등)만 씁니다.", nullable = true, example = "38")
    override val topPercent: Int?,
    @field:Schema(description = "큰 글자. 선물이면 '착한 아이', 석탄이면 '나쁜 아이'입니다.", example = "착한 아이")
    val verdict: String,
    @field:Schema(description = "선물을 받으면(65점 이상) true입니다. 재심은 없습니다.")
    val passed: Boolean,
    @field:Schema(description = "총점(점, 소수 둘째 자리). 점수 칩에 씁니다.", example = "73.73")
    val score: BigDecimal,
    @field:Schema(description = "선물 컷(점)", example = "65")
    val cut: Int,
    @field:JsonProperty("cut_distance")
    @field:Schema(description = "컷 거리 칩 표시. 부호를 늘 붙이고 소수 둘째 자리까지입니다.", example = "+8.73")
    val cutDistance: String,
    @field:JsonProperty("cut_distance_value")
    @field:Schema(description = "컷 거리 숫자(점). 컷 아래면 음수입니다.", example = "8.73")
    val cutDistanceValue: BigDecimal,
    @field:Schema(description = "구간 이름(배지)", example = "굴뚝에 걸쳐서 통과")
    val grade: String,
    @field:JsonProperty("grade_code")
    @field:Schema(description = "구간 코드", allowableValues = ["PREMIUM", "GIFT_DEPARTED", "SCRAPED_THROUGH", "ONE_STEP_SHORT", "OFF_LIST", "COAL"])
    val gradeCode: String,
    @field:JsonProperty("verdict_line")
    @field:Schema(description = "AI 한 줄(18자 이하, 숫자 없음). 옛 결과는 더 길 수 있습니다.")
    val verdictLine: String,
    @field:JsonProperty("delivery_eta")
    @field:Schema(description = "선물 도착 예정 시각(UTC). 석탄이거나 순위 계산 전이면 null입니다.", format = "date-time", nullable = true)
    val deliveryEta: String?,
) : RecapCardResponse {
    companion object {
        private const val KIND = "VERDICT"

        fun from(card: RecapVerdictCard, top: RecapTopResult, scoreText: String): VerdictCardResponse = VerdictCardResponse(
            card.index, KIND, RecapCardResponse.WIDTH, RecapCardResponse.HEIGHT, scoreText, top.rank, top.topPercent, card.verdict,
            card.passed, card.score, card.cut, card.cutDistance, card.cutDistanceValue, card.grade, card.gradeCode, card.verdictLine,
            card.deliveryEta?.toString(),
        )
    }
}
