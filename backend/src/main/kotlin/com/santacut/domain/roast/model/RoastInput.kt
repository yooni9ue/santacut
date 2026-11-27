package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 팩폭 작가에게 보내는 사용자 메시지 JSON. 숫자 값은 칩 후보의 참고 값([chips])에만 담는다.
 *
 * @property verdict gift 또는 coal
 * @property margin 컷과의 거리 말(넉넉, 아슬아슬, 한참). 등급 경계를 따른다.
 * @property gradeCode 등급 코드([com.santacut.domain.analysis.model.GiftGrade] 이름). 판정 한 줄의 구간 템플릿을 고른다.
 * @property temperature 등급 온도(넉넉한 통과, 아슬아슬 통과, 아깝게 탈락, 석탄)
 * @property facts 서버가 센 사실 목록. key가 곧 chip_key다.
 * @property chips 근거 카드에 고를 수 있는 칩 후보와 서버 값(참고용)
 * @property slots 서버가 정한 근거 카드 수. kind별로 정확히 이만큼 쓴다.
 * @property quotable 작은따옴표로 인용해도 되는 입버릇
 * @property topics 막별 화제 후보
 * @property transcript 막별 본인 말풍선(가린 뒤). 줄 앞 번호가 line_id다. 상대 말풍선은 넣지 않는다.
 * @property slang 써도 되는 유행어 표기 → 뜻(roast/slang.json). 목록 밖 유행어는 쓰지 않게 한다.
 * @property rewrite 다시 쓰기 요청. 첫 호출이면 싣지 않는다.
 * @property punchTemplates 이 사람에게 정해 준 결론 마지막 한 줄 펀치 틀(참가자·해 시드로 구간 풀을 돌린 자리). final_line은 이 틀을 쓴다.
 * @property verdictTemplate 이 사람에게 정해 준 판정 한 줄 틀(이 구간 풀에서 시드로 고른 것). 다른 구간의 틀은 보내지 않는다.
 * @property nearMiss 컷과 1점 안일 때의 말("컷 바로 위 0.13점 차"). 아니면 싣지 않는다.
 * @property signatureChipKey 이 사람을 가장 잘 말하는 대표 사실 칩. 근거 카드나 hero_chip_key 가운데 한 곳에 꼭 쓴다. 작가가 놓을 수 없는 칩이면 싣지 않는다.
 * @property bannedMarks 이 사람의 구간이 쓸 수 없는 다른 구간의 이름·밈. 판정 한 줄, title, final_line에 쓰면 떨어진다.
 */
data class RoastInput(
    val verdict: String,
    val margin: String,
    @JsonProperty("grade_code") val gradeCode: String,
    val temperature: String,
    val facts: RoastFacts,
    val chips: RoastChipHints,
    val slots: RoastSlots,
    val quotable: RoastQuotes,
    val topics: RoastTopics,
    val transcript: List<RoastTranscriptAct>,
    val slang: Map<String, String> = emptyMap(),
    @field:JsonInclude(JsonInclude.Include.NON_NULL) val rewrite: RoastRewrite? = null,
    @JsonProperty("punch_templates") val punchTemplates: List<String> = emptyList(),
    @field:JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("verdict_template") val verdictTemplate: String? = null,
    @field:JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("near_miss") val nearMiss: String? = null,
    @field:JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("signature_chip_key") val signatureChipKey: String? = null,
    @JsonProperty("banned_marks") val bannedMarks: List<String> = emptyList(),
)
