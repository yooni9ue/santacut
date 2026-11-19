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
            description = "근거가 부족해 사전값(중립 점수 55와 근거 있는 다른 항목 평균들의 평균)을 준 채점 항목 키. 이 항목 점수는 신뢰도가 낮습니다. 없으면 빈 배열입니다.",
        ),
        schema = Schema(
            allowableValues = ["care_empathy", "positivity_support", "respectful_communication", "cooperation_sharing"],
        ),
    )
    val lowConfidenceDimensions: List<String>,
    @field:Schema(description = "재미용 산타 유형. 가장 높은 항목과 장난기로 정하며 순위에는 영향이 없습니다.")
    val archetype: ArchetypeResponse,
    @field:JsonProperty("dimension_scores")
    @field:ArraySchema(
        arraySchema = Schema(description = "항목 막대 4개(공감·응원·존중·협력 순서). 수축을 거친 항목 점수입니다."),
        schema = Schema(implementation = DimensionBarResponse::class),
    )
    val dimensionScores: DimensionBarResponses,
    @field:JsonProperty("weakest_dimension")
    @field:Schema(description = "점수가 가장 낮은 항목")
    val weakestDimension: DimensionBarResponse,
    @field:JsonProperty("top_percent")
    @field:Schema(
        description = "상위 몇 %인지(1~100, 올림). 순위 계산 참가자가 100명 미만이거나 첫 순위 계산 전이면 null입니다.",
        nullable = true,
        example = "12",
    )
    val topPercent: Int?,
    @field:JsonProperty("archetype_share_percent")
    @field:Schema(
        description = "같은 유형인 참가자 비율(%, 반올림). 희귀도 표시에 씁니다. 점수를 받은 참가자가 30명보다 적으면 null이며, 이때는 숨깁니다.",
        nullable = true,
        example = "8",
    )
    val archetypeSharePercent: Int?,
    @field:ArraySchema(
        arraySchema = Schema(description = "본인 말에서 낱말 횟수로 받은 재미용 배지 라벨. 점수에는 영향이 없습니다. 없으면 빈 배열입니다."),
        schema = Schema(example = "고마워 12회 · 감사 자판기"),
    )
    val badges: List<String>,
    @field:Schema(
        description = "본인 근거 줄에서 고른 20자 이하 한마디. 상대 말은 들어가지 않습니다. 본인 결과에만 보이며 없으면 null입니다.",
        nullable = true,
    )
    val quote: String?,
    @field:JsonProperty("dossier_available")
    @field:Schema(
        description = "이 결과에 산타 조서(GET /v1/me/dossier)가 있으면 true입니다. 이 기능 전에 받은 결과면 false이고, 조서를 보려면 대화를 다시 올려야 합니다.",
    )
    val dossierAvailable: Boolean,
) {
    companion object {
        fun from(result: MyRankingResult): MyResultResponse = MyResultResponse(
            result.rank, result.tied, result.deliverySequence, result.eta?.toString(), result.computedAt?.toString(),
            result.scoreHundredths, result.verdict, result.comment, result.stage, result.rankingPending,
            result.lowConfidenceDimensions, ArchetypeResponse(result.archetypeCode, result.archetypeName),
            DimensionBarResponses.from(result.dimensionBars), DimensionBarResponse.from(result.weakestDimension),
            result.topPercent, result.archetypeSharePercent, result.badges, result.quote, result.dossierAvailable,
        )
    }
}
