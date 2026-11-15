package com.santacut.application.analysis.command

import java.util.UUID

/**
 * 참가자 본인이 가장 최근에 낸 분석 작업을 가리킨다.
 */
data class LatestAnalysisCommand(
    val participantId: UUID,
)
