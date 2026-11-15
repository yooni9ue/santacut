package com.santacut.application.result

import com.santacut.domain.model.AnalysisJob

/**
 * 접수한 분석 작업.
 *
 * @property status 작업 상태 이름
 */
data class AnalysisSubmissionResult(
    val id: String,
    val status: String,
) {
    companion object {
        fun from(job: AnalysisJob): AnalysisSubmissionResult = AnalysisSubmissionResult(job.id, job.status.name)
    }
}
