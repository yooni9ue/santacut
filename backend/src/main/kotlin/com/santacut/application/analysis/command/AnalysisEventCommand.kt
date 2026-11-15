package com.santacut.application.analysis.command

import com.santacut.domain.analysis.model.AnalysisEvent
import java.time.Instant

/**
 * 분석에 제출한 이벤트 하나.
 */
data class AnalysisEventCommand(
    val occurredAt: Instant,
    val text: String,
) {
    fun toEvent(): AnalysisEvent = AnalysisEvent(occurredAt, text)
}
