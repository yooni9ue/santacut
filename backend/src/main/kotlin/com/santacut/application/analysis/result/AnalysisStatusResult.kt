package com.santacut.application.analysis.result

import com.santacut.domain.analysis.model.AnalysisJob
import com.santacut.domain.analysis.model.AnalysisProgress
import java.time.Instant

/**
 * 분석 작업의 현재 상태.
 *
 * @property status 응답용 진행 단계 이름([AnalysisProgress])
 * @property errorCode 실패 이유 이름. 실패하기 전에는 null이다.
 */
data class AnalysisStatusResult(
    val id: String,
    val status: String,
    val errorCode: String?,
    val assessmentId: String?,
    val createdAt: Instant,
    val finishedAt: Instant?,
) {
    companion object {
        fun from(job: AnalysisJob, progress: AnalysisProgress): AnalysisStatusResult = AnalysisStatusResult(
            job.id, progress.name, job.errorCode?.name, job.assessmentId, job.createdAt, job.finishedAt,
        )
    }
}
