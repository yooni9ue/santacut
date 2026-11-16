package com.santacut.domain.analysis.model

import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

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

    /** [zone] 시각으로 바꾼 채점용 메시지. */
    fun toScoringMessage(zone: ZoneId): ScoringMessage = ScoringMessage(LocalDateTime.ofInstant(occurredAt, zone), text)

    companion object {
        /** 이벤트 본문 하나의 최대 글자 수. 원본 파일에서 읽은 더 긴 메시지는 이 길이로 자른다. */
        const val MAX_TEXT_LENGTH = 2000

        private val MAX_FUTURE_SKEW: Duration = Duration.ofSeconds(300)
        private val MAX_AGE: Duration = Duration.ofDays(90)
    }
}
