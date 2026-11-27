package com.santacut.application.roast.result

/**
 * 컷 표의 구간 한 칸.
 *
 * @property code 구간 코드([com.santacut.domain.analysis.model.GiftGrade] 이름)
 * @property label 구간 이름(굴뚝 프리패스 등)
 * @property range 점수 구간 표시("90+", "65~74", "≤49")
 * @property current 내 점수가 든 칸인지
 */
data class RecapBandResult(val code: String, val label: String, val range: String, val current: Boolean)
