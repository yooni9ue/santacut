package com.santacut.domain.dossier.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 웃음(ㅋ·ㅎ)과 울음(ㅠ·ㅜ) 글자 집계. 밀도는 본인 메시지 100개당 글자 수(소수 첫째 자리)다.
 *
 * @property laughMaxRun 가장 긴 ㅋ 연타와 그 달·시. ㅋ가 없으면 null이다.
 */
data class LaughStats(
    @JsonProperty("laugh_count") val laughCount: Int,
    @JsonProperty("laugh_density") val laughDensity: Double,
    @JsonProperty("laugh_max_run") val laughMaxRun: LaughRun?,
    @JsonProperty("cry_count") val cryCount: Int,
    @JsonProperty("cry_density") val cryDensity: Double,
)
