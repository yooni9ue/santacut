package com.santacut.application.analysis.result

import com.santacut.domain.analysis.model.AnalysisJob
import com.santacut.domain.analysis.model.AnalysisProgress

/**
 * 접수한 분석 작업.
 *
 * @property status 응답용 진행 단계 이름([AnalysisProgress])
 */
data class AnalysisSubmissionResult(
    val id: String,
    val status: String,
) {
    companion object {
        fun from(job: AnalysisJob, progress: AnalysisProgress): AnalysisSubmissionResult =
            AnalysisSubmissionResult(job.id, progress.name)
    }
}
