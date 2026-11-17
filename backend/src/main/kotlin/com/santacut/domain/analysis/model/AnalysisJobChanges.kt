package com.santacut.domain.analysis.model

/**
 * 한꺼번에 알릴 분석 작업 저장 알림 목록.
 */
class AnalysisJobChanges(private val values: List<AnalysisJobChange>) {
    /** 알림을 하나씩 [publish]로 보낸다. */
    fun publishEach(publish: (AnalysisJobChange) -> Unit) {
        values.forEach(publish)
    }
}
