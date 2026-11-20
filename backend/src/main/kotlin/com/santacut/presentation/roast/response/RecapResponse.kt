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
    @field:JsonProperty("share_text_mild")
    @field:Schema(
        description = "농담을 끈 사람의 공유 문구. 점수와 놀림 없이 통과면 '산타가 내 단톡방 다 읽음. 올해 선물 받음. 너도 까봐.', " +
            "탈락이면 '올해 선물은 내년으로 미뤄짐. 너는 받았는지 까봐.'입니다.",
        example = "올해 선물은 내년으로 미뤄짐. 너는 받았는지 까봐.",
    )
    val shareTextMild: String,
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
            result.assessmentId, result.skin, result.score, result.scoreText, result.shareText, result.shareTextMild, RecapCardResponses.from(result),
        )
    }
}
