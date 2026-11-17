package com.santacut.domain.analysis.model

/**
 * 순위를 다시 계산한 뒤 한꺼번에 알릴 분석 작업 저장 알림 목록. 진행 상황 스트림이 커밋 뒤 작업들을 한 번에 읽어 새 상태를 보낸다.
 */
class AnalysisJobChanges(private val values: List<AnalysisJobChange>) {
    fun isEmpty(): Boolean = values.isEmpty()

    fun size(): Int = values.size

    /** 알릴 작업 id. */
    fun jobIds(): List<String> = values.map(AnalysisJobChange::jobId)
}
