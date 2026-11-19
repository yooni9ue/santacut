package com.santacut.domain.dossier.model

/**
 * 장면에 붙이는 막대 그래프 종류. [type]은 응답의 chart.type이다.
 */
enum class ChartKind(val type: String) {
    HOURLY("bar24"),
    MONTHLY("bar12"),
    ;

    fun chartOf(stats: DossierStats): SceneChart = when (this) {
        HOURLY -> SceneChart(type, stats.clock.hourly)
        MONTHLY -> SceneChart(type, stats.clock.monthly)
    }
}
