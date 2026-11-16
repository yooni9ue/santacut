package com.santacut.domain.analysis.model

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 정규화한 이벤트 하나. 필드 이름과 순서는 저장 페이로드와 요청 해시의 형식이다.
 *
 * @property partner 다른 참여자가 보낸 메시지면 `상대`, `상대1` 같은 화자 표시, 본인 메시지면 null이다.
 * 원본 파일 제출에서만 채운다. null이면 JSON에 쓰지 않아 이벤트 제출의 요청 해시가 바뀌지 않는다.
 * 상대 메시지는 작업 페이로드에만 있으므로 작업이 끝나면 함께 지워진다.
 */
data class AnalysisEvent(
    val occurredAt: Instant,
    val text: String,
    @field:JsonInclude(JsonInclude.Include.NON_NULL)
    val partner: String? = null,
) {
    fun isSelf(): Boolean = partner == null

    /** 이벤트 시각이 최근 90일 안이고 5분 뒤보다 늦지 않으면 거짓이다. */
    fun isOutsideWindow(now: Instant): Boolean =
        occurredAt.isAfter(now.plus(MAX_FUTURE_SKEW)) || occurredAt.isBefore(now.minus(MAX_AGE))

    /** [zone] 시각으로 바꾼 채점용 메시지. */
    fun toScoringMessage(zone: ZoneId): ScoringMessage =
        ScoringMessage(LocalDateTime.ofInstant(occurredAt, zone), text, partner ?: ScoringMessage.SELF)

    companion object {
        /** 이벤트 본문 하나의 최대 글자 수. 원본 파일에서 읽은 더 긴 메시지는 이 길이로 자른다. */
        const val MAX_TEXT_LENGTH = 2000

        private val MAX_FUTURE_SKEW: Duration = Duration.ofSeconds(300)
        private val MAX_AGE: Duration = Duration.ofDays(90)
    }
}
