package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 작가 입력의 칩 후보 하나. 작가는 근거 카드마다 [chipKey] 하나를 고른다. [value]는 참고용이고 문장에 옮기면 숫자 검사에서 떨어진다.
 * 칩 글자는 서버가 [label]과 [value]로 만든다.
 *
 * @property quote 이 칩 카드에 붙여도 되는 말풍선 조건([QuoteRule.hintWith]). 조건에 맞지 않는 말풍선은 서버가 떨어뜨린다.
 * @property kinds 이 칩을 쓸 수 있는 근거 종류 key(칩 방향 [ChipDirection.kindKeys]). null이면 모든 종류에 쓸 수 있다.
 * 좋은 쪽은 `["reason","keep"]`, 나쁜 쪽(고마워 칩 포함)은 `["cut"]`, 중립은 빈 목록이라 근거 카드에 쓰지 않고 결론 대표 칩으로만 쓴다.
 */
data class RoastChipHint(
    @JsonProperty("chip_key") val chipKey: String,
    val label: String,
    val value: String,
    val quote: String,
    @field:JsonInclude(JsonInclude.Include.NON_NULL) val kinds: List<String>? = null,
)
