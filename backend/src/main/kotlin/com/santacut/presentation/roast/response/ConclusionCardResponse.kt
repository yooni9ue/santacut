package com.santacut.presentation.roast.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.santacut.application.roast.result.RecapConclusionCard
import com.santacut.application.roast.result.RecapTopResult
import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    description = "결론 카드(CONCLUSION). 덱의 마지막 장이자 공유 기본 카드입니다. 구간 이름(가장 큰 글자), 칭호·산타 유형, 대표 말풍선과 칩, 마지막 한 줄, " +
        "점수·컷 거리·상위 비율 칩과 미니 컷 표, 친구 소환 고정문과 해시태그를 싣습니다.",
)
data class ConclusionCardResponse(
    @field:Schema(description = "카드 순서(0부터)", example = "7")
    override val index: Int,
    @field:Schema(description = "카드 종류", allowableValues = ["CONCLUSION"])
    override val kind: String,
    @field:Schema(description = "카드 가로 픽셀", example = "1080")
    override val width: Int,
    @field:Schema(description = "카드 세로 픽셀", example = "1920")
    override val height: Int,
    @field:JsonProperty("score_text")
    @field:Schema(description = "총점 표시. 점수 칩에 씁니다.", example = "73.73")
    override val scoreText: String,
    @field:Schema(description = "상단 바의 전국 순위. 순위 반영 전이면 null입니다.", nullable = true)
    override val rank: Int?,
    @field:JsonProperty("top_percent")
    @field:Schema(description = "상단 바와 칩의 상위 비율(%). 순위가 없거나 참가자가 100명 아래면 null이고, 그때 상단 바는 순위(전국 N등)만 씁니다.", nullable = true)
    override val topPercent: Int?,
    @field:Schema(description = "판정 큰 글자(배지)", example = "착한 아이")
    val verdict: String,
    @field:Schema(description = "선물이면 true입니다. GIFT는 금색, COAL은 석탄색으로 그립니다.")
    val passed: Boolean,
    @field:Schema(description = "구간 이름. 덱에서 가장 큰 글자입니다.", example = "굴뚝에 걸쳐서 통과")
    val grade: String,
    @field:JsonProperty("grade_code")
    @field:Schema(description = "구간 코드", allowableValues = ["PREMIUM", "GIFT_DEPARTED", "SCRAPED_THROUGH", "ONE_STEP_SHORT", "OFF_LIST", "COAL"])
    val gradeCode: String,
    @field:JsonProperty("band_tag")
    @field:Schema(description = "해시태그 낱말. 화면은 '#산타컷 #' 뒤에 붙입니다.", example = "걸쳐서통과")
    val bandTag: String,
    @field:JsonProperty("band_position")
    @field:Schema(description = "미니 컷 표에서 내 칸의 자리(0부터, 0이 가장 높은 구간)", example = "2")
    val bandPosition: Int,
    @field:Schema(description = "밈형 칭호(8자 이하, AI 또는 구간별 대체 문장)", example = "턱걸이 장인")
    val title: String,
    @field:JsonProperty("archetype_code")
    @field:Schema(description = "산타 유형 코드")
    val archetypeCode: String,
    @field:JsonProperty("archetype_name")
    @field:Schema(description = "산타 유형 이름. 칭호 옆에 '칭호 · 유형'으로 씁니다.")
    val archetypeName: String,
    @field:JsonProperty("type_percent")
    @field:Schema(description = "같은 유형 참가자 비율(%). 참가자가 모자라면 null이고, 그때는 칩을 빼고 그립니다.", nullable = true)
    val typePercent: Int?,
    @field:JsonProperty("hero_quote")
    @field:Schema(description = "대표 본인 말풍선(이름 가림, 20자 이하). 근거 카드와 같아도 됩니다. 없으면 null입니다.", nullable = true)
    val heroQuote: String?,
    @field:JsonProperty("hero_chip_text")
    @field:Schema(description = "대표 칩 글자. 없으면 null입니다.", nullable = true)
    val heroChipText: String?,
    @field:JsonProperty("final_line")
    @field:Schema(description = "마지막 한 줄(22자 이하, 숫자 없음)")
    val finalLine: String,
    @field:JsonProperty("cut_distance")
    @field:Schema(description = "컷 거리 칩 표시", example = "+8.73")
    val cutDistance: String,
    @field:JsonProperty("friend_call")
    @field:Schema(description = "친구 소환 고정문(서버 상수). 선물이면 '우리 방에서 나보다 착한 애 나와', 석탄이면 '우리 방 석탄 동기 구함'입니다.")
    val friendCall: String,
) : RecapCardResponse {
    companion object {
        private const val KIND = "CONCLUSION"

        fun from(card: RecapConclusionCard, top: RecapTopResult, scoreText: String): ConclusionCardResponse = ConclusionCardResponse(
            card.index, KIND, RecapCardResponse.WIDTH, RecapCardResponse.HEIGHT, scoreText, top.rank, top.topPercent, card.verdict,
            card.passed, card.grade, card.gradeCode, card.bandTag, card.bandPosition, card.title, card.archetypeCode, card.archetypeName,
            card.archetypeSharePercent, card.heroQuote, card.heroChipText, card.finalLine, card.cutDistance, card.friendCall,
        )
    }
}
