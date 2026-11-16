package com.santacut.application.analysis.result

import com.santacut.domain.analysis.model.AnalysisJob
import com.santacut.domain.analysis.model.AnalysisProgress
import java.time.Instant

/**
 * 분석 작업의 현재 상태.
 *
 * @property status 응답용 진행 단계 이름([AnalysisProgress])
 * @property errorCode 실패 이유 이름. 실패하기 전에는 null이다.
 * @property eventId 진행 상황 스트림 이벤트 ID로 쓰는 진행 단계 순번([AnalysisProgress.ordinal])
 * @property terminal 더 바뀌지 않는 단계인지. 스트림은 이 단계를 보낸 뒤 닫는다.
 */
data class AnalysisStatusResult(
    val id: String,
    val status: String,
    val errorCode: String?,
    val assessmentId: String?,
    val createdAt: Instant,
    val finishedAt: Instant?,
    val eventId: Int,
    val terminal: Boolean,
) {
    companion object {
        fun from(job: AnalysisJob, progress: AnalysisProgress): AnalysisStatusResult = AnalysisStatusResult(
            job.id, progress.name, job.errorCode?.name, job.assessmentId, job.createdAt, job.finishedAt,
            progress.ordinal, progress.isTerminal(),
        )
    }
}
