package com.santacut.application.analysis.command

import java.util.UUID

/**
 * 참가자 본인의 분석 작업 하나에 진행 상황 스트림을 연다.
 *
 * @property openStreams 지금 열려 있는 스트림 수
 * @property replacing 이 참가자가 이미 연 스트림이 있어 새 스트림이 그것을 대신하는지
 */
data class OpenAnalysisStreamCommand(
    val participantId: UUID,
    val jobId: UUID,
    val openStreams: Int,
    val replacing: Boolean,
)
