package com.santacut.application.analysis.command

import java.util.UUID

/**
 * 참가자 본인의 분석 작업 하나를 가리킨다.
 */
data class AnalysisJobCommand(
    val participantId: UUID,
    val jobId: UUID,
)
