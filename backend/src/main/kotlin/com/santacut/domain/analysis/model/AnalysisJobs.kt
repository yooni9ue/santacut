package com.santacut.domain.analysis.model

/**
 * 한꺼번에 읽은 분석 작업 목록.
 */
class AnalysisJobs(private val values: List<AnalysisJob>) {
    /** 작업을 낸 참가자 id. 겹치지 않는다. */
    fun userIds(): Set<String> = values.map(AnalysisJob::userId).toSet()

    /** 읽은 순서대로 각 작업을 바꾼 결과. */
    fun <T> map(transform: (AnalysisJob) -> T): List<T> = values.map(transform)
}
