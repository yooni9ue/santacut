package com.santacut.application.analysis.command

/**
 * 진행 상황 스트림을 하나 더 열려고 한다.
 *
 * @property openStreams 지금 열려 있는 스트림 수
 * @property replacing 이 참가자가 이미 연 스트림이 있어 새 스트림이 그것을 대신하는지
 */
data class OpenAnalysisStreamCommand(
    val openStreams: Int,
    val replacing: Boolean,
)
