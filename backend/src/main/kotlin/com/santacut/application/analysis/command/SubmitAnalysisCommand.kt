package com.santacut.application.analysis.command

import com.santacut.domain.analysis.model.AnalysisSubmission
import com.santacut.domain.analysis.model.DataSource
import java.util.UUID

/**
 * 분석 작업을 접수한다.
 *
 * @property source 요청 검증을 통과한 [DataSource] 이름
 * @property clientIp 요청한 클라이언트 IP. HTTP 밖에서 부르면 null이고 IP 한도를 세지 않는다.
 */
data class SubmitAnalysisCommand(
    val participantId: UUID,
    val idempotencyKey: String,
    val source: String,
    val events: AnalysisEventCommands,
    val clientIp: String? = null,
) {
    fun toSubmission(): AnalysisSubmission =
        AnalysisSubmission(DataSource.valueOf(source), events.toEvents())
}
