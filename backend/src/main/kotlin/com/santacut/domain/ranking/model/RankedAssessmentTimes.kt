package com.santacut.domain.ranking.model

import java.time.Instant

/**
 * 여러 참가자의 최신 순위 반영 평가 시각. 순위 계산 뒤 진행 상황을 한꺼번에 알릴 때 한 번에 읽는다.
 */
class RankedAssessmentTimes(values: List<RankedAssessmentTime>) {
    private val values: Map<String, Instant> = values.associate { ranked -> ranked.userId to ranked.assessedAt }

    /** [userId] 참가자의 최신 순위 반영 평가 시각. 순위에 오른 적이 없으면 null이다. */
    fun of(userId: String): Instant? = values[userId]
}
