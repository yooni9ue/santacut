package com.santacut.domain.analysis.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import java.time.Instant

/**
 * 분석에 제출한 이벤트 목록. JSON에서는 배열 하나로 읽고 쓴다.
 */
class AnalysisEvents @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(
    private val values: List<AnalysisEvent>,
) {
    fun size(): Int = values.size

    /** JSON 배열로 쓸 이벤트 목록의 방어 복사본. */
    @JsonValue
    fun toList(): List<AnalysisEvent> = values.toList()

    /** 제출 순서대로 1부터 시작하는 번호를 붙여 각 이벤트를 바꾼다. */
    fun <T> mapNumbered(transform: (Int, AnalysisEvent) -> T): List<T> =
        values.mapIndexed { index, event -> transform(index + FIRST_NUMBER, event) }

    /** 이벤트 수, 전체 텍스트 길이, 이벤트 시각 범위를 확인한다. */
    fun verifyAcceptableAt(now: Instant) {
        if (values.size < MIN_EVENTS) throw SantaCutException(ErrorCode.INSUFFICIENT_EVENTS)
        if (values.size > MAX_EVENTS) throw SantaCutException(ErrorCode.TOO_MANY_EVENTS)
        if (totalTextLength() > MAX_TOTAL_TEXT_LENGTH || values.any { it.isOutsideWindow(now) }) {
            throw SantaCutException(ErrorCode.INVALID_EVENT_RANGE)
        }
    }

    private fun totalTextLength(): Int = values.sumOf { it.text.length }

    companion object {
        /** 한 번에 받는 이벤트 수의 하한과 상한. */
        const val MIN_EVENTS = 20
        const val MAX_EVENTS = 2000
        private const val MAX_TOTAL_TEXT_LENGTH = 200_000
        private const val FIRST_NUMBER = 1
    }
}
