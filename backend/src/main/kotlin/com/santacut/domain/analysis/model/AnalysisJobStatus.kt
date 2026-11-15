package com.santacut.domain.analysis.model

/**
 * 분석 작업 상태.
 */
enum class AnalysisJobStatus {
    QUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    INSUFFICIENT_DATA,
    ;

    /** 응답용 진행 단계. 성공했어도 아직 순위에 반영되지 않았으면([ranked]가 거짓) 순위 계산 대기다. */
    fun toProgress(ranked: Boolean): AnalysisProgress = when (this) {
        QUEUED -> AnalysisProgress.QUEUED
        RUNNING -> AnalysisProgress.ANALYZING
        SUCCEEDED -> if (ranked) AnalysisProgress.SUCCEEDED else AnalysisProgress.RANKING_PENDING
        FAILED, INSUFFICIENT_DATA -> AnalysisProgress.FAILED
    }
}
