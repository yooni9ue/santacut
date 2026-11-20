package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 놀림(roast)과 뒤집기(flip) 한 짝. [factKey]는 근거가 된 사실 key다(대체 문장이 사실 없이 쓰였으면 generic).
 *
 * @property chip 그 사실의 서버 숫자 칩. 사실 없이 쓴 대체 짝이면 null이다.
 */
data class RoastPair(
    @JsonProperty("fact_key") val factKey: String,
    val roast: String,
    val flip: String,
    val source: RoastSource = RoastSource.AI,
    val chip: RoastChip? = null,
)
