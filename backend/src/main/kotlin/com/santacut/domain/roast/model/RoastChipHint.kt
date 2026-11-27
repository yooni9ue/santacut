package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 작가 입력의 칩 후보 하나. 작가는 근거 카드마다 [chipKey] 하나를 고른다. [value]는 참고용이고 문장에 옮기면 숫자 검사에서 떨어진다.
 * 칩 글자는 서버가 [label]과 [value]로 만든다.
 */
data class RoastChipHint(
    @JsonProperty("chip_key") val chipKey: String,
    val label: String,
    val value: String,
)
