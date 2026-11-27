package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * 작가 유행어 목록(roast/slang.json). 허용 목록은 작가 입력으로 나가고, 금지 목록은 출력 검증의 금지어에 더한다.
 * 12월 초처럼 유행이 바뀌면 이 파일만 고쳐 작가 입력과 검증을 함께 바꾼다.
 *
 * @property updated 목록을 마지막으로 고친 날(YYYY-MM-DD)
 * @property maxPerField 한 필드에 쓸 수 있는 허용 유행어 수
 * @property maxPerDeck 리캡 덱 전체(판정 한 줄, 근거 카드, 결론)에 쓸 수 있는 허용 유행어 수
 * @property bannedPatterns 낱말 경계가 필요한 금지 말 정규식(테토·에겐처럼 흔한 말에 섞이는 것)
 */
data class RoastSlang(
    val updated: String,
    val sources: List<String>,
    @JsonProperty("max_per_field") val maxPerField: Int,
    @JsonProperty("max_per_deck") val maxPerDeck: Int,
    val allowed: SlangWords,
    val banned: List<String>,
    @JsonProperty("banned_patterns") val bannedPatterns: List<String>,
    val notes: List<String> = emptyList(),
) {
    fun violations(): List<String> = listOfNotNull(
        "slang.updated: YYYY-MM-DD 날짜여야 한다".takeUnless { isDate(updated) },
        "slang.max_per_field: 1 이상이다".takeIf { maxPerField < 1 },
        "slang.max_per_deck: max_per_field 이상이다".takeIf { maxPerDeck < maxPerField },
    ) + allowed.violations(banned)

    private fun isDate(text: String): Boolean = try {
        LocalDate.parse(text)
        true
    } catch (exception: DateTimeParseException) {
        false
    }

    companion object {
        /** 유행어를 쓰지 않는 목록. 테스트·옛 설정에서 쓴다. */
        val EMPTY = RoastSlang(EMPTY_DATE, emptyList(), 1, 1, SlangWords.EMPTY, emptyList(), emptyList())
        private const val EMPTY_DATE = "2026-01-01"
    }
}
