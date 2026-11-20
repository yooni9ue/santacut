package com.santacut.domain.dossier.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 본인 메시지의 양.
 *
 * @property messageCount 본인 메시지 수(사진·이모티콘 같은 자리표시 포함)
 * @property textCount 자리표시와 빈 메시지를 뺀 글 메시지 수. 밀도·비율·특이도의 분모다.
 * @property charCount 글 메시지의 글자 수(코드 포인트, 이모지 하나가 한 글자)
 * @property activeDays 본인 메시지가 하나라도 있는 날 수(하루 경계 05:00)
 * @property busiestDay 메시지가 가장 많은 날의 개수와 그 달. 메시지가 없으면 null이다.
 */
data class VolumeStats(
    @JsonProperty("message_count") val messageCount: Int,
    @JsonProperty("text_count") val textCount: Int,
    @JsonProperty("char_count") val charCount: Int,
    @JsonProperty("active_days") val activeDays: Int,
    @JsonProperty("busiest_day") val busiestDay: BusiestDay?,
) {
    /** 사진·이모티콘 같은 자리표시와 빈 메시지의 비율(0~1). 메시지가 없으면 null이다. */
    fun mediaRatio(): Double? = if (messageCount == 0) null else DossierRatios.ratio(messageCount - textCount, messageCount)

    /** 말풍선을 보낸 날 하루에 보낸 평균 개수. 활동일이 없으면 null이다. */
    fun messagesPerDay(): Double? = if (activeDays == 0) null else DossierRatios.average(messageCount.toLong(), activeDays)

    /** 가장 많이 보낸 날의 개수가 활동일 하루 평균의 몇 배인지(소수 첫째 자리). 메시지가 없으면 null이다. */
    fun busiestDayMultiple(): Double? = busiestDay?.takeIf { messageCount > 0 }?.let { day ->
        DossierRatios.tenth(day.count.toDouble() * activeDays / messageCount)
    }
}
