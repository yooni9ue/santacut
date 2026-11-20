package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 팩폭 작가에게 보내는 사용자 메시지 JSON. 숫자 값은 담지 않는다.
 *
 * @property verdict gift 또는 coal
 * @property margin 컷과의 거리 말(넉넉, 아슬아슬, 한참). 등급 경계를 따른다.
 * @property gradeCode 등급 코드([com.santacut.domain.analysis.model.GiftGrade] 이름)
 * @property temperature 등급 온도(넉넉한 통과, 아슬아슬 통과, 아깝게 탈락, 석탄). 판결의 온도는 이것을 따른다.
 * @property quotable 작은따옴표로 인용해도 되는 입버릇
 * @property topics 막별 화제 후보
 * @property transcript 막별 본인 말풍선(가린 뒤)
 * @property rewrite 다시 쓰기 요청. 첫 호출이면 싣지 않는다.
 */
data class RoastInput(
    val verdict: String,
    val margin: String,
    @JsonProperty("grade_code") val gradeCode: String,
    val temperature: String,
    val facts: RoastFacts,
    val quotable: RoastQuotes,
    val topics: RoastTopics,
    val transcript: List<RoastTranscriptAct>,
    @field:JsonInclude(JsonInclude.Include.NON_NULL) val rewrite: RoastRewrite? = null,
)
