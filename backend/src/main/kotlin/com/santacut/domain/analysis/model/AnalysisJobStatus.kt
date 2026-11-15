package com.santacut.domain.analysis.model

/**
 * 분석 작업 상태. [VALIDATING]은 원본 파일로 받아 아직 검증·정규화하지 않은 작업이다.
 */
enum class AnalysisJobStatus {
    VALIDATING,
    QUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    INSUFFICIENT_DATA,
    ;

    /** 응답용 진행 단계. 성공했어도 아직 순위에 반영되지 않았으면([ranked]가 거짓) 순위 계산 대기다. */
    fun toProgress(ranked: Boolean): AnalysisProgress = when (this) {
        VALIDATING -> AnalysisProgress.VALIDATING
        QUEUED -> AnalysisProgress.QUEUED
        RUNNING -> AnalysisProgress.ANALYZING
        SUCCEEDED -> if (ranked) AnalysisProgress.SUCCEEDED else AnalysisProgress.RANKING_PENDING
        FAILED, INSUFFICIENT_DATA -> AnalysisProgress.FAILED
    }

    companion object {
        /** 너무 오래 기다리면 만료하는 상태. 검증 대기와 점수 산정 대기다. */
        val EXPIRABLE: Set<AnalysisJobStatus> = setOf(VALIDATING, QUEUED)
    }
}
