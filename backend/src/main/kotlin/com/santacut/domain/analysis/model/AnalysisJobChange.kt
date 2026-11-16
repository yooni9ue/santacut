package com.santacut.domain.analysis.model

/**
 * 분석 작업 행을 저장했다는 알림. 진행 상황 스트림이 커밋 뒤 이 작업의 새 상태를 보낸다.
 * 상태가 그대로인 저장도 알릴 수 있으므로 받는 쪽이 같은 상태를 걸러 낸다.
 */
data class AnalysisJobChange(
    val jobId: String,
)
