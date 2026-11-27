package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 근거 카드 종류. 작가 출력과 저장 JSON에는 [key](reason, cut, keep)로 쓴다.
 * REASON은 선물 받는 이유, CUT은 점수 깎인 말, KEEP은 석탄 덱에서 살아남은 말이다.
 *
 * @property lineOptional 본인 말 없이(line_id null) 칩만으로 써도 되는지. 점수 깎인 말만 그렇다.
 */
enum class EvidenceKind(val key: String, val lineOptional: Boolean) {
    @JsonProperty("reason")
    REASON("reason", false),

    @JsonProperty("cut")
    CUT("cut", true),

    @JsonProperty("keep")
    KEEP("keep", false),
    ;

    companion object {
        /** 작가 출력의 kind 글자로 찾는다. 모르는 값이면 null이다. */
        fun ofKey(key: String): EvidenceKind? = entries.find { kind -> kind.key == key }
    }
}
