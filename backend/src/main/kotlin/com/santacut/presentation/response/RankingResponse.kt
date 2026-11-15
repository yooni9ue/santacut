package com.santacut.presentation.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.santacut.application.result.RankingRowResult
import io.swagger.v3.oas.annotations.media.Schema

data class RankingResponse(
    @field:Schema(description = "참가자 닉네임")
    val nickname: String,
    @field:Schema(description = "경쟁 순위. 점수가 같으면 같은 순위입니다.")
    val rank: Int,
    @field:JsonProperty("score_hundredths")
    @field:Schema(description = "100분의 1점 단위 가중 점수. 100으로 나누면 점수입니다.")
    val scoreHundredths: Int,
    @field:Schema(description = "시뮬레이션한 배송 도착 예정 시각(UTC)", format = "date-time")
    val eta: String,
    @field:JsonProperty("computed_at")
    @field:Schema(description = "순위 스냅숏을 계산한 시각(UTC)", format = "date-time")
    val computedAt: String,
) {
    companion object {
        fun from(result: RankingRowResult): RankingResponse = RankingResponse(
            result.nickname, result.rank, result.scoreHundredths, result.eta.toString(), result.computedAt.toString(),
        )
    }
}
