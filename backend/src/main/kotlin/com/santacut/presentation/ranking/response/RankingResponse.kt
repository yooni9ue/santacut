package com.santacut.presentation.ranking.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.santacut.application.ranking.result.RankingRowResult
import io.swagger.v3.oas.annotations.media.Schema

data class RankingResponse(
    @field:Schema(description = "참가자 닉네임")
    val nickname: String,
    @field:Schema(description = "경쟁 순위. 점수가 같으면 같은 순위입니다.")
    val rank: Int,
    @field:Schema(description = "같은 순위의 다른 참가자가 있으면 true입니다(공동 N위로 표시).")
    val tied: Boolean,
    @field:JsonProperty("score_hundredths")
    @field:Schema(description = "100분의 1점 단위 가중 점수. 100으로 나누면 점수입니다.")
    val scoreHundredths: Int,
    @field:Schema(description = "시뮬레이션한 배송 도착 예정 시각(UTC)", format = "date-time")
    val eta: String,
) {
    companion object {
        fun from(result: RankingRowResult): RankingResponse = RankingResponse(
            result.nickname, result.rank, result.tied, result.scoreHundredths, result.eta.toString(),
        )
    }
}
