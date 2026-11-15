package com.santacut.presentation.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.santacut.application.result.AnalysisOutcomeResult
import io.swagger.v3.oas.annotations.media.Schema

data class AnalysisResultResponse(
    @field:JsonProperty("score_hundredths")
    @field:Schema(
        description = "100분의 1점 단위 가중 점수. 100으로 나누면 점수입니다.",
        example = "8245",
        minimum = MIN_SCORE_HUNDREDTHS,
        maximum = MAX_SCORE_HUNDREDTHS,
    )
    val scoreHundredths: Int,
    @field:Schema(description = "재미용 점수 판정")
    val verdict: String,
    @field:Schema(description = "재미용 한마디")
    val comment: String,
    @field:Schema(description = "경쟁 순위. 점수가 같으면 같은 순위입니다.")
    val rank: Int,
    @field:JsonProperty("delivery_sequence")
    @field:Schema(description = "참가자 사이의 배송 순서")
    val deliverySequence: Int,
    @field:Schema(
        description = "시뮬레이션한 배송 도착 예정 시각(UTC)",
        type = "string",
        format = "date-time",
        example = "2026-12-25T03:15:00.000Z",
    )
    val eta: String,
) {
    companion object {
        /** 100분의 1점 단위 점수의 하한과 상한(0점~100점). Swagger 스키마가 문자열로 받는다. */
        const val MIN_SCORE_HUNDREDTHS = "0"
        const val MAX_SCORE_HUNDREDTHS = "10000"

        fun from(result: AnalysisOutcomeResult): AnalysisResultResponse = AnalysisResultResponse(
            result.scoreHundredths, result.verdict, result.comment,
            result.rank, result.deliverySequence, result.eta.toString(),
        )
    }
}
