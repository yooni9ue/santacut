package com.santacut.domain.analysis.model

/**
 * 화면에 보여 줄 분석 진행 단계. 저장하는 작업 상태([AnalysisJobStatus])를 응답용 어휘로 바꾼 값이다.
 * [VALIDATING]은 원본 파일 검사 단계용으로 남겨 둔 값이며 지금 서버는 내보내지 않는다.
 */
enum class AnalysisProgress {
    VALIDATING,
    QUEUED,
    ANALYZING,
    RANKING_PENDING,
    SUCCEEDED,
    FAILED,
}
