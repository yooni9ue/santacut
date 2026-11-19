package com.santacut.domain.dossier.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 칭호 부품(copy.json의 `titles`). 칭호는 극값 1위 지표의 수식어와 극값 2위 지표의 직함을 잇는다.
 */
data class TitleCopy(
    @JsonProperty("modifier_by_metric") val modifierByMetric: KeyedTemplates,
    @JsonProperty("role_by_metric") val roleByMetric: KeyedTemplates,
    @JsonProperty("default_modifier") val defaultModifier: Templates,
    @JsonProperty("default_role") val defaultRole: Templates,
)
