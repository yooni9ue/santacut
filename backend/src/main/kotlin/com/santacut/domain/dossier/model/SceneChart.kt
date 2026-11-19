package com.santacut.domain.dossier.model

/**
 * 장면의 막대 그래프. [type]은 bar24(시간대 24칸)나 bar12(달 12칸)다.
 */
data class SceneChart(val type: String, val values: List<Int>)
