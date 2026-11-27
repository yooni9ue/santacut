package com.santacut.presentation.roast.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.santacut.application.roast.result.RecapResult
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

data class RecapResponse(
    @field:JsonProperty("assessment_id")
    @field:Schema(description = "카드를 만든 결과(평가) ID. 최신 평가입니다.")
    val assessmentId: String,
    @field:Schema(description = "카드 스킨. 선물이면 GIFT, 석탄이면 COAL입니다.", allowableValues = ["GIFT", "COAL"])
    val skin: String,
    @field:Schema(description = "총점(점, 소수 둘째 자리)", example = "73.73")
    val score: BigDecimal,
    @field:JsonProperty("score_text")
    @field:Schema(description = "총점 표시", example = "73.73")
    val scoreText: String,
    @field:JsonProperty("share_text")
    @field:Schema(
        description = "서버가 고정한 공유 문구. 등급마다 다르고 총점 표시(score_text)가 들어갑니다.",
        example = "산타가 내 단톡방 다 읽음. 73.73점, 굴뚝에 걸쳐서 겨우 통과. 넌 넘냐?",
    )
    val shareText: String,
    @field:Schema(description = "전국 순위. 카드마다 같은 값이 있습니다. 순위 계산이 끝나면 참가자 수와 상관없이 늘 있고, 최신 결과가 아직 순위에 반영되지 않았으면 null입니다.", nullable = true)
    val rank: Int?,
    @field:JsonProperty("top_percent")
    @field:Schema(description = "상위 비율(%, 1~100). 순위와 그 순위 계산의 참가자 수로 냅니다. 순위가 없으면 null입니다.", nullable = true)
    val topPercent: Int?,
    @field:JsonProperty("ranking_pending")
    @field:Schema(description = "최신 결과가 아직 순위에 반영되지 않았으면 true입니다. 이때는 이 API를 다시 불러 순위가 들어올 때까지 기다립니다.")
    val rankingPending: Boolean,
    @field:ArraySchema(
        arraySchema = Schema(
            description = "카드 목록(순서 고정, 5~8장, 모두 1080x1920). 선물 덱: VERDICT, CUTLINE, REASON(최대 3), CUT(최대 2), CONCLUSION. " +
                "석탄 덱: VERDICT, CUTLINE, CUT(최대 3), KEEP(1), CONCLUSION.",
        ),
        schema = Schema(
            oneOf = [
                VerdictCardResponse::class, CutlineCardResponse::class, ReasonCardResponse::class, CutCardResponse::class,
                KeepCardResponse::class, ConclusionCardResponse::class,
            ],
        ),
    )
    val cards: RecapCardResponses,
) {
    companion object {
        fun from(result: RecapResult): RecapResponse = RecapResponse(
            result.assessmentId, result.skin, result.score, result.scoreText, result.shareText, result.top.rank, result.top.topPercent,
            result.top.rankingPending, RecapCardResponses.from(result),
        )
    }
}
