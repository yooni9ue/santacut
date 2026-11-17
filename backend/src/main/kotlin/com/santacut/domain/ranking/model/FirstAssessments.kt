package com.santacut.domain.ranking.model

import java.time.Instant

/**
 * 참가자별 시즌 첫 심사 성공 시각 목록.
 */
class FirstAssessments(values: List<FirstAssessment>) {
    private val values: Map<String, Instant> = values.associate { first -> first.userId to first.assessedAt }

    /** [userId] 참가자의 첫 심사 성공 시각. 모르면 null이다. */
    fun of(userId: String): Instant? = values[userId]

    companion object {
        val EMPTY = FirstAssessments(emptyList())
    }
}
