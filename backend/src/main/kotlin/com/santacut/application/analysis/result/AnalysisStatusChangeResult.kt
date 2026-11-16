package com.santacut.application.analysis.result

import java.util.UUID

/**
 * 작업 상태가 바뀐 트랜잭션이 커밋된 뒤 진행 상황 스트림에 알리는 새 상태.
 *
 * @property participantId 작업을 낸 참가자
 */
data class AnalysisStatusChangeResult(
    val participantId: UUID,
    val status: AnalysisStatusResult,
)
