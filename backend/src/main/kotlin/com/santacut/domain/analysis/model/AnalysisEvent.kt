package com.santacut.domain.analysis.model

import java.time.Duration
import java.time.Instant

/**
 * 정규화한 이벤트 하나. 필드 이름과 순서는 저장 페이로드와 요청 해시의 형식이다.
 */
data class AnalysisEvent(
    val occurredAt: Instant,
    val text: String,
) {
    /** 이벤트 시각이 최근 90일 안이고 5분 뒤보다 늦지 않으면 거짓이다. */
    fun isOutsideWindow(now: Instant): Boolean =
        occurredAt.isAfter(now.plus(MAX_FUTURE_SKEW)) || occurredAt.isBefore(now.minus(MAX_AGE))

    companion object {
        private val MAX_FUTURE_SKEW: Duration = Duration.ofSeconds(300)
        private val MAX_AGE: Duration = Duration.ofDays(90)
    }
}
