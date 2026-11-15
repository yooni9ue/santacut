package com.santacut.application.command

import com.santacut.domain.model.AnalysisEvents

/**
 * 분석에 제출한 이벤트 목록. 제출 순서를 유지한다.
 */
class AnalysisEventCommands(private val values: List<AnalysisEventCommand>) {
    fun toEvents(): AnalysisEvents = AnalysisEvents(values.map { it.toEvent() })
}
