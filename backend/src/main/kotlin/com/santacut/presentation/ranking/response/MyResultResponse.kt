package com.santacut.presentation.ranking.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.santacut.application.ranking.result.MyRankingResult
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema

data class MyResultResponse(
    @field:Schema(description = "경쟁 순위. 점수가 같으면 같은 순위입니다. 첫 순위 계산 전이면 null입니다.")
    val rank: Int?,
    @field:Schema(description = "같은 순위의 다른 참가자가 있으면 true입니다(공동 N위로 표시).")
    val tied: Boolean,
    @field:JsonProperty("delivery_sequence")
    @field:Schema(description = "참가자 사이의 배송 순서. 첫 순위 계산 전이면 null입니다.", nullable = true)
    val deliverySequence: Int?,
    @field:Schema(description = "시뮬레이션한 배송 도착 예정 시각(UTC). 첫 순위 계산 전이면 null입니다.", format = "date-time")
    val eta: String?,
    @field:JsonProperty("computed_at")
    @field:Schema(description = "순위 스냅숏을 계산한 시각(UTC). 첫 순위 계산 전이면 null입니다.", format = "date-time", nullable = true)
    val computedAt: String?,
    @field:JsonProperty("score_hundredths")
    @field:Schema(description = "100분의 1점 단위 가중 점수. 100으로 나누면 점수입니다.", example = "8245")
    val scoreHundredths: Int,
    @field:Schema(description = "재미용 점수 판정")
    val verdict: String,
    @field:Schema(description = "재미용 한마디")
    val comment: String,
    @field:Schema(
        description = "순위 확정 상태. PREVIEW는 시즌 마감 전 미리보기, FINAL은 마감 후 확정입니다.",
        allowableValues = ["PREVIEW", "FINAL"],
    )
    val stage: String,
    @field:JsonProperty("ranking_pending")
    @field:Schema(description = "최신 심사 결과가 아직 순위에 반영되지 않았으면 true입니다. 점수와 순위는 늘 같은 심사 것입니다.")
    val rankingPending: Boolean,
    @field:JsonProperty("low_confidence_dimensions")
    @field:ArraySchema(
        arraySchema = Schema(
            description = "근거가 부족해 중립 점수(55점)를 준 채점 항목 키. 이 항목 점수는 신뢰도가 낮습니다. 없으면 빈 배열입니다.",
        ),
        schema = Schema(
            allowableValues = ["care_empathy", "positivity_support", "respectful_communication", "cooperation_sharing"],
        ),
    )
    val lowConfidenceDimensions: List<String>,
) {
    companion object {
        fun from(result: MyRankingResult): MyResultResponse = MyResultResponse(
            result.rank, result.tied, result.deliverySequence, result.eta?.toString(), result.computedAt?.toString(),
            result.scoreHundredths, result.verdict, result.comment, result.stage, result.rankingPending,
            result.lowConfidenceDimensions,
        )
    }
}
