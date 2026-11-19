package com.santacut.domain.dossier.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 산타 조서 장 문구(copy.json의 `dossier`).
 *
 * @property charges 통계 key별 죄목
 * @property merits 따뜻한 말 key나 통계 key별 공로
 * @property summaryByBand 판정 구간(top, high, mid, low)별 총평
 */
data class CaseFileCopy(
    val header: Templates,
    val charges: KeyedTemplates,
    val merits: KeyedTemplates,
    @JsonProperty("summary_by_band") val summaryByBand: KeyedTemplates,
)
