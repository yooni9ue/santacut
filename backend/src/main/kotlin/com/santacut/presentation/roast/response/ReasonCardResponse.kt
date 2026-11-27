package com.santacut.presentation.roast.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.santacut.application.roast.result.RecapEvidenceCard
import com.santacut.application.roast.result.RecapTopResult
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "선물 받는 이유 카드(REASON). 선물 덱에만 최대 세 장 들어갑니다. 본인 말풍선, 서버 칩, 상 이름 라벨, 한 줄, 영역 태그를 싣습니다.")
data class ReasonCardResponse(
    @field:Schema(description = "카드 순서(0부터)", example = "2")
    override val index: Int,
    @field:Schema(description = "카드 종류", allowableValues = ["REASON"])
    override val kind: String,
    @field:Schema(description = "카드 가로 픽셀", example = "1080")
    override val width: Int,
    @field:Schema(description = "카드 세로 픽셀", example = "1920")
    override val height: Int,
    @field:JsonProperty("score_text")
    @field:Schema(description = "총점 표시", example = "73.73")
    override val scoreText: String,
    @field:Schema(description = "상단 바의 전국 순위. 순위 반영 전이면 null입니다.", nullable = true)
    override val rank: Int?,
    @field:JsonProperty("top_percent")
    @field:Schema(description = "상단 바의 상위 비율(%). 순위가 없거나 참가자가 100명 아래면 null이고, 그때 상단 바는 순위(전국 N등)만 씁니다.", nullable = true)
    override val topPercent: Int?,
    @field:Schema(description = "같은 종류 안 순번(1부터). 키커 '선물 받는 이유 {n}'의 n입니다.", example = "1")
    val ordinal: Int,
    @field:Schema(description = "본인 말풍선. 서버가 원문에서 이름을 ○○로 가리고 전화번호·링크를 지운 뒤 20자에서 자른 것입니다. null이면 말풍선 없이 칩을 크게 찍는 통계 카드입니다.", nullable = true)
    val quote: String?,
    @field:JsonProperty("chip_text")
    @field:Schema(description = "서버 칩 글자. 숫자는 서버가 센 값입니다.", example = "일정·장소 제안 27번")
    val chipText: String,
    @field:Schema(description = "상 이름 꼴 라벨(8자 이하)", example = "약속발사상")
    val label: String,
    @field:Schema(description = "한 줄(20자 이하, 숫자 없음)")
    val line: String,
    @field:JsonProperty("dim_tag")
    @field:Schema(description = "영역 태그. 칩이 가리키는 항목(항목 점수 칩, 지표·칭찬 입버릇 칩의 이어 둔 항목)에 '+'를 붙여 싣고, 이어 둔 항목이 없는 칩은 null입니다.", nullable = true, example = "협력 +")
    val dimTag: String?,
) : RecapCardResponse {
    companion object {
        private const val KIND = "REASON"

        fun from(card: RecapEvidenceCard, top: RecapTopResult, scoreText: String): ReasonCardResponse = ReasonCardResponse(
            card.index, KIND, RecapCardResponse.WIDTH, RecapCardResponse.HEIGHT, scoreText, top.rank, top.topPercent, card.ordinal,
            card.quote, card.chipText, card.label, card.line, card.dimTag,
        )
    }
}
