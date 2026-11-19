package com.santacut.application.dossier.result

import com.santacut.domain.dossier.model.SceneChart

/**
 * 장면의 막대 그래프. [type]은 bar24나 bar12다.
 */
data class DossierChartResult(val type: String, val values: List<Int>) {
    companion object {
        fun from(chart: SceneChart): DossierChartResult = DossierChartResult(chart.type, chart.values)
    }
}
