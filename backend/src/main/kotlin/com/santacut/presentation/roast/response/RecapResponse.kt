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
    @field:Schema(description = "서버가 고정한 공유 문구", example = "산타 공식 인증. 올해 선물 받음. 너는?")
    val shareText: String,
    @field:ArraySchema(
        arraySchema = Schema(
            description = "카드 목록(순서 고정): VERDICT, ROAST 세 장, LETTER, STORY(있을 때만), ARCHETYPE, DIMENSIONS. 모두 1080x1920(9:16)입니다.",
        ),
        schema = Schema(
            oneOf = [
                VerdictCardResponse::class, RoastCardResponse::class, LetterCardResponse::class, StoryCardResponse::class,
                ArchetypeCardResponse::class, DimensionsCardResponse::class,
            ],
        ),
    )
    val cards: RecapCardResponses,
) {
    companion object {
        fun from(result: RecapResult): RecapResponse = RecapResponse(
            result.assessmentId, result.skin, result.score, result.scoreText, result.shareText, RecapCardResponses.from(result),
        )
    }
}
