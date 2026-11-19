package com.santacut.domain.dossier.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 본인 메시지의 시각 분포(한국 시각). 요일·달은 하루 경계 05:00으로 센다(새벽 4시 메시지는 전날).
 *
 * @property hourly 0~23시별 개수
 * @property monthly 1~12월별 개수
 * @property weekday 월~일요일별 개수
 * @property peakHour 가장 많이 보낸 시. 메시지가 없으면 null이다.
 * @property nightRatio 0~5시(5시 전)에 보낸 비율(0~1)
 * @property busiestMonth 가장 많이 보낸 달. 메시지가 없으면 null이다.
 * @property quietestMonth 메시지가 있는 달 중 가장 적게 보낸 달
 * @property monthRatio 가장 많은 달 개수 ÷ 가장 적은 달 개수(소수 첫째 자리)
 */
data class ClockStats(
    val hourly: List<Int>,
    val monthly: List<Int>,
    val weekday: List<Int>,
    @JsonProperty("peak_hour") val peakHour: Int?,
    @JsonProperty("night_ratio") val nightRatio: Double,
    @JsonProperty("busiest_month") val busiestMonth: Int?,
    @JsonProperty("quietest_month") val quietestMonth: Int?,
    @JsonProperty("month_ratio") val monthRatio: Double?,
)
