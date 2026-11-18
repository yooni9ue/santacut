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
        val selfCount = values.count { it.isSelf() }
        if (selfCount < MIN_EVENTS) throw SantaCutException(ErrorCode.INSUFFICIENT_EVENTS)
        if (selfCount > MAX_EVENTS || values.size > MAX_TOTAL_EVENTS) throw SantaCutException(ErrorCode.TOO_MANY_EVENTS)
        if (totalTextLength() > MAX_TOTAL_TEXT_LENGTH || values.any { it.isOutsideWindow(now) }) {
            throw SantaCutException(ErrorCode.INVALID_EVENT_RANGE)
        }
    }

    /**
     * 원본 파일에서 뽑은 이벤트를 제출 한도에 맞춘다. 올해 1월 1일 전이거나 5분 넘게 미래인 이벤트는 버리고 시각순으로 놓는다.
     * 그다음 [SpreadCandidates] 규칙으로 달마다 고르게 고르는데, 늘 본인 이벤트를 먼저 고르고 상대 이벤트는 남은 예산으로 고른다.
     * 이벤트 수는 본인 [MAX_EVENTS]개, 상대 포함 [MAX_TOTAL_EVENTS]개까지, 글자 수는 본인 [MAX_SELF_TEXT_LENGTH]자,
     * 전체 [MAX_TOTAL_TEXT_LENGTH]자까지다. 점수는 본인 말만 평가하므로, 상대가 길게 많이 말하는 단체방에서도 본인 말이 밀려나지 않게 한다.
     * 본인과 상대는 달마다 따로 최신 구간을 남기므로 두 구간의 시작이 조금 어긋날 수 있다. 같은 시각 이벤트는 원래 순서를 지킨다.
     */
    fun selectedAt(now: Instant): AnalysisEvents {
        val inPeriod = values.filterNot { it.isOutsideWindow(now) }.sortedBy { it.occurredAt }
        val selves = spread(inPeriod, MAX_EVENTS) { event -> if (event.isSelf()) 1 else 0 }
        val partners = spread(selves, MAX_TOTAL_EVENTS - selves.count { it.isSelf() }) { event -> if (event.isSelf()) 0 else 1 }
        val selfTexts = spread(partners, selfTextBudget(partners)) { event -> if (event.isSelf()) event.text.length else 0 }
        val selfLength = selfTexts.filter { it.isSelf() }.sumOf { it.text.length }
        return AnalysisEvents(spread(selfTexts, MAX_TOTAL_TEXT_LENGTH - selfLength) { event -> if (event.isSelf()) 0 else event.text.length })
    }

    private fun totalTextLength(): Int = values.sumOf { it.text.length }

    /** 본인 글자 수 예산. 기본은 [MAX_SELF_TEXT_LENGTH]자이고, 상대 말이 짧아 남는 몫이 있으면 본인에게 준다. */
    private fun selfTextBudget(events: List<AnalysisEvent>): Int =
        maxOf(MAX_SELF_TEXT_LENGTH, MAX_TOTAL_TEXT_LENGTH - events.filterNot { it.isSelf() }.sumOf { it.text.length })

    /** 몫 합이 [budget] 이하면 후보를 만들지 않고 그대로 돌려준다. 1년치 단체방 원본에서 메모리를 아끼려는 것이다. */
    private fun spread(events: List<AnalysisEvent>, budget: Int, cost: (AnalysisEvent) -> Int): List<AnalysisEvent> {
        if (events.sumOf(cost) <= budget) return events
        return SpreadCandidates(events.map { it.toSpreadCandidate(cost(it)) }).within(budget)
    }

    companion object {
        /** 한 번에 받는 본인 이벤트 수의 하한과 상한. */
        const val MIN_EVENTS = 20
        const val MAX_EVENTS = 2000

        /** 상대 메시지를 포함한 이벤트 수의 상한. 본인 이벤트 하나에 상대 이벤트 두 개꼴의 문맥을 남긴다. */
        const val MAX_TOTAL_EVENTS = 6000
        private const val MAX_TOTAL_TEXT_LENGTH = 200_000

        /** 상대 말이 충분히 있을 때 본인 글자 수가 먼저 쓰는 몫. 전체의 70%라 본인 말이 아무리 길어도 상대 말이 문맥으로 남는다. */
        private const val MAX_SELF_TEXT_LENGTH = 140_000
    }
}
