package com.santacut.domain.ranking.model

import java.time.Instant

/**
 * 참가자의 최신 순위 스냅숏이 담은 평가를 만든 시각. 리포지토리 조회 결과를 바로 담는다.
 * 이 시각 뒤에 끝난 심사는 아직 순위에 반영되지 않았다.
 */
data class RankedAssessmentTime(
    val userId: String,
    val assessedAt: Instant,
)
