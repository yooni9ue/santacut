package com.santacut.domain.model

/**
 * 분석 작업 상태.
 */
enum class AnalysisJobStatus {
    QUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    INSUFFICIENT_DATA,
}
