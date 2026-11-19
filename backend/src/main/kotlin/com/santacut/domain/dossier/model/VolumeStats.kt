package com.santacut.domain.dossier.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 본인 메시지의 양.
 *
 * @property activeDays 본인 메시지가 하나라도 있는 날 수(하루 경계 05:00)
 * @property busiestDay 메시지가 가장 많은 날의 개수와 그 달. 메시지가 없으면 null이다.
 */
data class VolumeStats(
    @JsonProperty("message_count") val messageCount: Int,
    @JsonProperty("char_count") val charCount: Int,
    @JsonProperty("active_days") val activeDays: Int,
    @JsonProperty("busiest_day") val busiestDay: BusiestDay?,
)
