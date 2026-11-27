package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 다시 쓰기 입력에 싣는, 이미 받아 둔 근거 카드 하나. 작가는 이 카드와 line_id·chip_key·낱말·라벨 첫머리가 겹치지 않게 빈자리만 채운다.
 *
 * @property kind 근거 종류 key(reason, cut, keep)
 * @property lineId 고른 본인 줄 번호. 말풍선 없는 카드면 null이다.
 */
data class KeptCard(
    val kind: String,
    @field:JsonInclude(JsonInclude.Include.ALWAYS) @JsonProperty("line_id") val lineId: Int?,
    @JsonProperty("chip_key") val chipKey: String,
    val label: String,
    val line: String,
)
