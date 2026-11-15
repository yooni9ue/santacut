package com.santacut.presentation.ranking.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.santacut.application.ranking.result.RankingBoardResult
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema

data class RankingBoardResponse(
    @field:JsonProperty("season_year")
    @field:Schema(description = "시즌 연도", example = "2026")
    val seasonYear: Int,
    @field:JsonProperty("participant_count")
    @field:Schema(description = "가장 최근 순위 계산에 오른 참가자 수. 순위가 없으면 0입니다.", example = "128")
    val participantCount: Long,
    @field:JsonProperty("computed_at")
    @field:Schema(description = "가장 최근 순위 계산 시각(UTC). 순위가 없으면 null입니다.", format = "date-time", nullable = true)
    val computedAt: String?,
    @field:Schema(
        description = "순위 확정 상태. PREVIEW는 시즌 마감 전 미리보기, FINAL은 마감 후 확정입니다.",
        allowableValues = ["PREVIEW", "FINAL"],
    )
    val stage: String,
    @field:ArraySchema(
        arraySchema = Schema(description = "배송 순서대로 최대 50줄"),
        schema = Schema(implementation = RankingResponse::class),
    )
    val rows: RankingResponses,
    @field:JsonProperty("next_cursor")
    @field:Schema(description = "다음 페이지를 읽을 불투명 문자열. 마지막 페이지면 null입니다.", nullable = true)
    val nextCursor: String?,
) {
    companion object {
        fun from(result: RankingBoardResult): RankingBoardResponse = RankingBoardResponse(
            result.seasonYear, result.participantCount, result.computedAt?.toString(), result.stage,
            RankingResponses.from(result.rows), result.nextCursor,
        )
    }
}
