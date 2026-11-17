package com.santacut.domain.ranking.model

import java.time.Instant

/**
 * 참가자가 이번 시즌 처음 심사에 성공한 시각. 공동 순위 안의 배송 순서를 정한다. 리포지토리 조회 결과를 바로 담는다.
 */
data class FirstAssessment(
    val userId: String,
    val assessedAt: Instant,
)
