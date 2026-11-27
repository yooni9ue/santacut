package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 구간(선물 판정 등급) 하나의 고정 대체 문장(roast/fallback.json의 bands). 작가 문장이 검증을 끝내 통과하지 못하면 쓴다.
 * 유행어가 든 문장과 유행어 없는 문장([plainVerdictLine], [plainFinalLine])을 함께 두고, 덱 전체 유행어 상한을 넘을 때 유행어 없는 쪽을 쓴다.
 *
 * @property verdictLine 판정 카드 한 줄(18자 이하)
 * @property title 결론 칭호(8자 이하)
 * @property finalLine 결론 마지막 한 줄(22자 이하)
 */
data class RoastBandCopy(
    @JsonProperty("verdict_line") val verdictLine: String,
    @JsonProperty("plain_verdict_line") val plainVerdictLine: String,
    val title: String,
    @JsonProperty("final_line") val finalLine: String,
    @JsonProperty("plain_final_line") val plainFinalLine: String,
) {
    fun verdictLine(plain: Boolean): String = if (plain) plainVerdictLine else verdictLine

    fun finalLine(plain: Boolean): String = if (plain) plainFinalLine else finalLine

    /** 모든 문장. 배포 전에 검증 규칙을 통과하는지 테스트가 확인한다. */
    fun all(): List<String> = listOf(verdictLine, plainVerdictLine, title, finalLine, plainFinalLine)
}
