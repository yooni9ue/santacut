package com.santacut.domain.dossier.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 답장 간격. 상대 메시지 뒤 10분 이상 조용했다가 온 본인 첫 메시지의 간격만 잰다.
 *
 * @property oneOnOne 상대 화자가 한 명인 1:1 대화인지
 * @property medianMinutes 간격의 중앙값(분). 1:1이 아니거나 표본이 10개보다 적으면 null이다.
 */
data class ReplyStats(
    @JsonProperty("one_on_one") val oneOnOne: Boolean,
    @JsonProperty("median_minutes") val medianMinutes: Int?,
)
