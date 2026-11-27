package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 저장한 결론 카드 문장. 칭호와 마지막 한 줄은 작가 문장이거나 구간별 대체 문장이고, 대표 말풍선과 칩은 서버가 채운다.
 * 대표 말풍선과 칩은 근거 카드에서 이미 쓴 것이어도 된다(덱 안 재사용 금지의 유일한 예외).
 *
 * @property title 밈형 칭호(8자 이하)
 * @property heroLineId 대표 본인 줄 번호. 본인 말이 없으면 null이다.
 * @property heroChipKey 대표 칩 key. 칩 후보가 없으면 null이다.
 * @property heroQuote 대표 말풍선(이름 가림, 20자 이하). 없으면 null이다.
 * @property heroChip 대표 칩 값. 없으면 null이다.
 * @property finalLine 마지막 한 줄(22자 이하, 숫자 없음)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class RoastConclusion(
    val title: RoastText,
    @JsonProperty("hero_line_id") val heroLineId: Int?,
    @JsonProperty("hero_chip_key") val heroChipKey: String?,
    @JsonProperty("hero_quote") val heroQuote: String?,
    @JsonProperty("hero_chip") val heroChip: RoastChip?,
    @JsonProperty("final_line") val finalLine: RoastText,
) {
    /** 대체 문장으로 채운 문장 수(칭호, 마지막 한 줄). */
    fun fallbackCount(): Int = title.fallbackCount() + finalLine.fallbackCount()

    companion object {
        /** 문장 수(칭호, 마지막 한 줄). */
        const val TEXTS = 2
    }
}
