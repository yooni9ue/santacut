package com.santacut.domain.analysis.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import java.time.Instant
import java.time.ZoneId

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

    /** 채점 대화문을 만들 시각순 메시지 목록으로 바꾼다. 같은 시각이면 받은 순서를 지킨다. 시각은 [zone] 기준이다. */
    fun toScoringMessages(zone: ZoneId): ScoringMessages =
        ScoringMessages(values.sortedBy { it.occurredAt }.map { it.toScoringMessage(zone) })

    /** 본인 이벤트 수, 전체 이벤트 수, 전체 텍스트 길이, 이벤트 시각 범위를 확인한다. */
    fun verifyAcceptableAt(now: Instant) {
        if (values.count { it.isSelf() } < MIN_EVENTS) throw SantaCutException(ErrorCode.INSUFFICIENT_EVENTS)
        if (values.size > MAX_EVENTS) throw SantaCutException(ErrorCode.TOO_MANY_EVENTS)
        if (totalTextLength() > MAX_TOTAL_TEXT_LENGTH || values.any { it.isOutsideWindow(now) }) {
            throw SantaCutException(ErrorCode.INVALID_EVENT_RANGE)
        }
    }

    /**
     * 원본 파일에서 뽑은 이벤트를 제출 한도에 맞춘다. 최근 90일 밖이거나 5분 넘게 미래인 이벤트는 버리고 시각순으로 놓는다.
     * 그다음 [MAX_EVENTS]개를 고르는데, 최신 본인 이벤트 [SELF_RESERVED_EVENTS]개에 먼저 자리를 주고 남은 자리는 나머지
     * 이벤트 가운데 최신 것으로 채운다. 상대가 많은 단체방에서 최신 2,000개만 남기면 본인 이벤트가 [MIN_EVENTS]개에 못 미쳐
     * 분석할 수 없게 되기 때문이다. 마지막으로 최신 이벤트부터 전체 글자 수 [MAX_TOTAL_TEXT_LENGTH]자까지만 남긴다.
     * 같은 시각 이벤트는 원래 순서를 지킨다.
     */
    fun recentAt(now: Instant): AnalysisEvents {
        val newestFirst = withinCount(values.filterNot { it.isOutsideWindow(now) }.sortedBy { it.occurredAt }).asReversed()
        val withinLength = newestFirst.runningFold(0) { total, event -> total + event.text.length }
            .drop(1)
            .count { it <= MAX_TOTAL_TEXT_LENGTH }
        return AnalysisEvents(newestFirst.take(withinLength).asReversed())
    }

    private fun totalTextLength(): Int = values.sumOf { it.text.length }

    /** 시각순 [sorted]에서 최신 본인 이벤트를 먼저 남기고 나머지 자리를 최신 이벤트로 채운 [MAX_EVENTS]개 이하. 시각순 그대로다. */
    private fun withinCount(sorted: List<AnalysisEvent>): List<AnalysisEvent> {
        val reserved = sorted.indices.filter { sorted[it].isSelf() }.takeLast(SELF_RESERVED_EVENTS)
        val others = (sorted.indices - reserved.toSet()).takeLast(MAX_EVENTS - reserved.size)
        return (reserved + others).sorted().map(sorted::get)
    }

    companion object {
        /** 한 번에 받는 본인 이벤트 수의 하한과, 상대 메시지를 포함한 이벤트 수의 상한. */
        const val MIN_EVENTS = 20
        const val MAX_EVENTS = 2000
        private const val MAX_TOTAL_TEXT_LENGTH = 200_000

        /** 원본을 제출 한도에 맞출 때 최신 본인 이벤트에 먼저 주는 자리 수. 상한의 절반이라 상대 이벤트도 문맥으로 충분히 남는다. */
        const val SELF_RESERVED_EVENTS = MAX_EVENTS / 2
    }
}
