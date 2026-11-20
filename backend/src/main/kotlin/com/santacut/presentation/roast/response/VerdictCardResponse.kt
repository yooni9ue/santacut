package com.santacut.presentation.roast.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.santacut.application.roast.result.RecapResult
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(description = "판정 카드(VERDICT). 점수, 컷 거리, 도장, 등급, 순위, 도착 시각, 칭호와 판결을 싣습니다.")
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
    @field:Schema(description = "모든 카드 상단에 찍는 총점 표시", example = "73.73")
    override val scoreText: String,
    @field:Schema(description = "총점(점, 소수 둘째 자리)", example = "73.73")
    val score: BigDecimal,
    @field:Schema(description = "선물 컷(점)", example = "65")
    val cut: Int,
    @field:JsonProperty("cut_distance")
    @field:Schema(description = "컷 거리 표시. 부호를 늘 붙이고 소수 둘째 자리까지입니다.", example = "+8.73")
    val cutDistance: String,
    @field:JsonProperty("cut_distance_value")
    @field:Schema(description = "컷 거리 숫자(점). 컷 아래면 음수입니다.", example = "8.73")
    val cutDistanceValue: BigDecimal,
    @field:Schema(description = "선물을 받으면(65점 이상) true입니다. 재심은 없습니다.")
    val passed: Boolean,
    @field:Schema(description = "도장 문구. 선물이면 '선물 배송 확정', 아니면 '석탄'입니다.", example = "선물 배송 확정")
    val stamp: String,
    @field:JsonProperty("stamp_mild")
    @field:Schema(description = "농담을 끈 사람의 도장 문구. 석탄 대신 '선물은 내년에'입니다.", example = "선물 배송 확정")
    val stampMild: String,
    @field:Schema(description = "등급 이름(특급, 선물 배송 출발, 턱걸이 통과, 한 끗, 명단 밖, 석탄)", example = "턱걸이 통과")
    val grade: String,
    @field:JsonProperty("grade_code")
    @field:Schema(
        description = "등급 코드",
        allowableValues = ["PREMIUM", "GIFT_DEPARTED", "SCRAPED_THROUGH", "ONE_STEP_SHORT", "OFF_LIST", "COAL"],
    )
    val gradeCode: String,
    @field:Schema(description = "전국 순위. 순위 계산 참가자가 표본 기준(상위 비율과 같은 100명)보다 적거나 계산 대기면 null입니다.", nullable = true)
    val rank: Int?,
    @field:JsonProperty("ranking_pending")
    @field:Schema(description = "최신 심사 결과가 아직 순위에 반영되지 않았으면 true입니다.")
    val rankingPending: Boolean,
    @field:JsonProperty("delivery_eta")
    @field:Schema(description = "선물 도착 예정 시각(UTC). 석탄이거나 순위 계산 전이면 null입니다.", format = "date-time", nullable = true)
    val deliveryEta: String?,
    @field:Schema(description = "칭호", example = "물음표 신호탄 요정")
    val title: String,
    @field:Schema(description = "판결 한 줄(농담)")
    val verdict: String,
    @field:JsonProperty("verdict_mild")
    @field:Schema(description = "놀림 없는 판결. 농담을 끈 사람에게 보입니다.")
    val verdictMild: String,
) : RecapCardResponse {
    companion object {
        private const val KIND = "VERDICT"

        fun from(index: Int, result: RecapResult): VerdictCardResponse {
            val verdict = result.verdict
            return VerdictCardResponse(
                index, KIND, RecapCardResponse.WIDTH, RecapCardResponse.HEIGHT, result.scoreText, result.score, verdict.cut,
                verdict.cutDistance, verdict.cutDistanceValue, verdict.passed, verdict.stamp, verdict.stampMild, verdict.grade,
                verdict.gradeCode, verdict.rank, verdict.rankingPending, verdict.deliveryEta?.toString(), verdict.title,
                verdict.verdict, verdict.verdictMild,
            )
        }
    }
}
