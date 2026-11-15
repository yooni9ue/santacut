package com.santacut.domain.analysis.model

/**
 * 화면에 보여 줄 분석 진행 단계. 저장하는 작업 상태([AnalysisJobStatus])를 응답용 어휘로 바꾼 값이다.
 * [VALIDATING]은 올린 원본 파일을 검증·정규화하는 단계다. JSON 이벤트로 낸 작업은 이 단계를 거치지 않는다.
 */
enum class AnalysisProgress {
    VALIDATING,
    QUEUED,
    ANALYZING,
    RANKING_PENDING,
    SUCCEEDED,
    FAILED,
}
