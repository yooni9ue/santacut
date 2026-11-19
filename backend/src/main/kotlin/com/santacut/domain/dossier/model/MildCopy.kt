package com.santacut.domain.dossier.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 담백 톤과 놀릴 수 없는 지표에 쓰는 중립 문구(copy.json의 `mild`).
 */
data class MildCopy(
    @JsonProperty("extremes_memo") val extremesMemo: Templates,
    val summary: Templates,
)
