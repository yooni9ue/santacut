package com.santacut.presentation.analysis.request

import com.santacut.application.analysis.command.AnalysisEventCommands

/**
 * 분석 요청 본문의 이벤트 목록. 제출 순서를 유지한다.
 */
class AnalysisEventRequests(private val values: List<AnalysisEventRequest>) {
    fun toCommands(): AnalysisEventCommands = AnalysisEventCommands(values.map { it.toCommand() })
}
