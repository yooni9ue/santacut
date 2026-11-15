package com.santacut.application.analysis.result

import com.santacut.domain.analysis.model.AnalysisJob
import java.time.Instant

/**
 * 분석 작업의 현재 상태.
 *
 * @property status 작업 상태 이름
 * @property errorCode 실패 이유 이름. 실패하기 전에는 null이다.
 * @property succeeded 성공해 결과를 볼 수 있는지
 * @property failed 실패나 근거 부족으로 끝났는지
 */
data class AnalysisStatusResult(
    val id: String,
    val status: String,
    val errorCode: String?,
    val assessmentId: String?,
    val createdAt: Instant,
    val finishedAt: Instant?,
    val succeeded: Boolean,
    val failed: Boolean,
) {
    companion object {
        fun from(job: AnalysisJob): AnalysisStatusResult = AnalysisStatusResult(
            job.id, job.status.name, job.errorCode?.name, job.assessmentId,
            job.createdAt, job.finishedAt, job.hasSucceeded(), job.hasFailed(),
        )
    }
}
