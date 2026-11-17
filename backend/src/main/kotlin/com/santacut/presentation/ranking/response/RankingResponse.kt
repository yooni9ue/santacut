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
    @field:JsonProperty("eta_date")
    @field:Schema(description = "선물 도착 예정일(Asia/Seoul 날짜). 다른 참가자의 정확한 시각은 공개하지 않습니다.", format = "date", example = "2026-12-26")
    val etaDate: String,
    @field:Schema(
        description = "더 쓰지 않는 필드로 늘 null입니다. 예전에는 정확한 도착 예정 시각이었으나 지역 추정을 막으려고 eta_date로 바꿨습니다.",
        deprecated = true,
        nullable = true,
    )
    val eta: String? = null,
) {
    companion object {
        fun from(result: RankingRowResult): RankingResponse = RankingResponse(
            result.nickname, result.rank, result.tied, result.scoreHundredths, result.etaDate.toString(),
        )
    }
}
