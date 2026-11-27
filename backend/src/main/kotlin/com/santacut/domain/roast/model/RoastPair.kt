package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 옛 팩폭 결과(작가 프롬프트 1~4판)의 놀림·뒤집기 짝 하나. 읽기 전용이고, 새 덱은 [chip]만 쓴다.
 *
 * @property factKey 근거가 된 사실 key(대체 문장이 사실 없이 쓰였으면 generic)
 * @property chip 그 사실의 서버 숫자 칩. 사실 없이 쓴 대체 짝이면 null이다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class RoastPair(
    @JsonProperty("fact_key") val factKey: String,
    val chip: RoastChip? = null,
)
