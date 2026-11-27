package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * 구간이 가진 표현 하나(roast/punch.json의 band_marks). 구간 이름('프리패스', '유턴')이나 그 구간에만 맞는 밈('체포하겠', '줴줴이야')이다.
 * [bands]에 들지 않은 구간의 판정 한 줄·결론 칭호·마지막 한 줄이 이 표현을 쓰면 떨어진다. 75~89점 덱에 90점 이상 구간 이름이,
 * 49점 이하 덱에 55~64점 구간 밈(줴줴이야)이 붙지 않게 한다.
 *
 * @property mark 작가 입력 banned_marks에 싣는 표현. [pattern]이 없으면 따옴표 인용 밖에서 이 글자를 그대로 찾는다.
 * @property bands 이 표현을 쓸 수 있는 등급 이름([com.santacut.domain.analysis.model.GiftGrade.name])
 * @property pattern 글자 그대로 찾을 수 없는 표현의 정규식. '올해 너는 ○○이야~' 틀처럼 빈칸이 있거나 '엄…'·'엄...'처럼 표기가 흔들리는 표현에 쓴다. 없으면 null이다.
 */
data class BandMark(
    val mark: String,
    val bands: List<String>,
    @field:JsonInclude(JsonInclude.Include.NON_NULL) val pattern: String? = null,
) {
    private val regex: Regex? = pattern?.let(::Regex)

    /** [text]에 이 표현이 있는지. */
    fun isIn(text: String): Boolean = regex?.containsMatchIn(text) ?: (mark in text)
}
