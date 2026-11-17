package com.santacut.application.analysis.result

import com.santacut.domain.analysis.model.AnalysisJobs
import com.santacut.domain.ranking.model.RankedAssessmentTimes
import java.util.UUID

/**
 * 순위 계산 뒤 진행 상황 스트림에 한꺼번에 알릴 새 상태 목록.
 */
class AnalysisStatusChangeResults(private val values: List<AnalysisStatusChangeResult>) {
    /** 하나씩 [publish]로 보낸다. */
    fun publishEach(publish: (AnalysisStatusChangeResult) -> Unit) {
        values.forEach(publish)
    }

    companion object {
        /** 작업마다 그 참가자의 최신 순위 반영 시각([rankedTimes])으로 진행 단계를 정한다. */
        fun of(jobs: AnalysisJobs, rankedTimes: RankedAssessmentTimes): AnalysisStatusChangeResults =
            AnalysisStatusChangeResults(
                jobs.map { job ->
                    val progress = job.progress(rankedTimes.of(job.userId))
                    AnalysisStatusChangeResult(UUID.fromString(job.userId), AnalysisStatusResult.from(job, progress))
                },
            )
    }
}
