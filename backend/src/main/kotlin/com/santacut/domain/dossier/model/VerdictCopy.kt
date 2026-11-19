package com.santacut.domain.dossier.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 판결 장 문구(copy.json의 `verdict`).
 *
 * @property reasonByDimension 최고 점수 항목별 판결 사유
 * @property note 통계 key별 주석
 * @property lowBandTitle 하위 판정 구간에만 붙이는 줄. 카드에서 기본으로 가린다.
 */
data class VerdictCopy(
    @JsonProperty("reason_by_dimension") val reasonByDimension: KeyedTemplates,
    val note: KeyedTemplates,
    @JsonProperty("low_band_title") val lowBandTitle: Templates,
)
