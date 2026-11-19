package com.santacut.domain.dossier.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 본인 메시지 모양. 길이와 비율은 사진·이모티콘 같은 자리표시를 뺀 글 메시지로 센다.
 *
 * @property avgLength 평균 글자 수(소수 첫째 자리)
 * @property shortRatio 5자 이하 메시지 비율(0~1)
 * @property burstAvg 3분 안에 잇달아 보낸 본인 메시지 묶음의 평균 개수(소수 첫째 자리)
 * @property burstMax 가장 긴 묶음의 개수
 * @property questionRatio 물음표가 든 메시지 비율(0~1)
 * @property exclaimRatio 느낌표가 든 메시지 비율(0~1)
 * @property longestMessageLength 가장 긴 메시지의 글자 수
 */
data class MessageStyle(
    @JsonProperty("avg_length") val avgLength: Double,
    @JsonProperty("short_ratio") val shortRatio: Double,
    @JsonProperty("burst_avg") val burstAvg: Double,
    @JsonProperty("burst_max") val burstMax: Int,
    @JsonProperty("question_ratio") val questionRatio: Double,
    @JsonProperty("exclaim_ratio") val exclaimRatio: Double,
    @JsonProperty("longest_message_length") val longestMessageLength: Int,
)
