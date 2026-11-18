package com.santacut.domain.analysis.model

import com.santacut.ErrorCodeAssertions.assertErrorCode
import com.santacut.domain.analysis.model.AnalysisEvents.Companion.MAX_EVENTS
import com.santacut.domain.analysis.model.AnalysisEvents.Companion.MAX_TOTAL_EVENTS
import com.santacut.domain.analysis.model.AnalysisEvents.Companion.MIN_EVENTS
import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import com.santacut.domain.global.model.SeoulTime
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AnalysisEventsTest {
    private val now = Instant.parse("2026-11-15T12:00:00Z")
    private val utcNoon = Instant.parse("2026-11-01T12:00:00Z")

    @Test
    fun `올해 1월 1일 0시(한국 시각)부터 5분 뒤까지의 이벤트는 받아들인다`() {
        val events = padded(
            listOf(
                AnalysisEvent(YEAR_START, "가장 오래된 이벤트"),
                AnalysisEvent(now.plusSeconds(300), "가장 늦은 이벤트"),
            ),
        )
        events.verifyAcceptableAt(now)
        assertEquals(MIN_EVENTS, events.size())
    }

    @Test
    fun `5분 넘게 미래인 이벤트가 있으면 범위 오류다`() {
        assertRejected(padded(listOf(AnalysisEvent(now.plusSeconds(301), "미래 이벤트"))))
    }

    @Test
    fun `올해 1월 1일 0시(한국 시각)보다 앞선 이벤트가 있으면 범위 오류다`() {
        assertRejected(padded(listOf(AnalysisEvent(YEAR_START.minusMillis(1), "작년 이벤트"))))
    }

    @Test
    fun `전체 텍스트가 200,000자를 넘으면 범위 오류다`() {
        val longText = "가".repeat(2000)
        val withinLimit = AnalysisEvents(List(100) { AnalysisEvent(now, longText) })
        withinLimit.verifyAcceptableAt(now)
        assertRejected(AnalysisEvents(List(100) { AnalysisEvent(now, longText) } + AnalysisEvent(now, "한")))
    }

    @Test
    fun `JSON에서는 이벤트 배열 하나로 쓰고 읽는다`() {
        val mapper = jacksonObjectMapper()
        val eventList = listOf(AnalysisEvent(now, "고마워"), AnalysisEvent(now.minusSeconds(60), "괜찮아"))
        val json = mapper.writeValueAsString(AnalysisEvents(eventList))
        assertEquals(mapper.writeValueAsString(eventList), json)
        assertEquals(eventList, mapper.readValue(json, AnalysisEvents::class.java).toList())
    }

    @Test
    fun `채점용 메시지로 바꿀 때 시각을 주어진 시간대로 옮기고 본문, 화자, 순서를 지킨다`() {
        val events = AnalysisEvents(listOf(AnalysisEvent(utcNoon, "첫째", "상대"), AnalysisEvent(utcNoon, "둘째")))

        val turns = events.toScoringMessages(SeoulTime.ZONE).toTurns()
        val lines = turns.mapNumbered { _, turn, _ -> "${turn.speaker}:${turn.text}" }

        assertEquals(listOf("상대:첫째", "나:둘째"), lines)
        assertEquals(LocalDateTime.of(2026, 11, 1, 21, 0), turns.mapNumbered { _, turn, _ -> turn.startedAt }.first())
    }

    @Test
    fun `본인 이벤트는 화자 필드 없이 JSON으로 써 이벤트 제출의 요청 해시가 바뀌지 않는다`() {
        val mapper = jacksonObjectMapper()
        val json = mapper.writeValueAsString(AnalysisEvents(listOf(AnalysisEvent(now, "고마워"), AnalysisEvent(now, "응", "상대"))))

        assertEquals(1, Regex("partner").findAll(json).count())
        assertEquals(AnalysisEvent(now, "응", "상대"), mapper.readValue(json, AnalysisEvents::class.java).toList()[1])
    }

    @Test
    fun `이벤트 수 하한은 본인 이벤트로만 센다`() {
        val own = List(MIN_EVENTS - 1) { AnalysisEvent(now, "고마워") }
        val partner = List(MIN_EVENTS) { AnalysisEvent(now, "응", "상대") }

        assertErrorCode(ErrorCode.INSUFFICIENT_EVENTS) { AnalysisEvents(own + partner).verifyAcceptableAt(now) }
    }

    @Test
    fun `이벤트가 20개보다 적으면 INSUFFICIENT_EVENTS, 2,000개보다 많으면 TOO_MANY_EVENTS다`() {
        val event = AnalysisEvent(now, "고마워")
        AnalysisEvents(List(MIN_EVENTS) { event }).verifyAcceptableAt(now)
        AnalysisEvents(List(MAX_EVENTS) { event }).verifyAcceptableAt(now)
        val tooFew = AnalysisEvents(List(MIN_EVENTS - 1) { event })
        val tooMany = AnalysisEvents(List(MAX_EVENTS + 1) { event })
        assertErrorCode(ErrorCode.INSUFFICIENT_EVENTS) { tooFew.verifyAcceptableAt(now) }
        assertErrorCode(ErrorCode.TOO_MANY_EVENTS) { tooMany.verifyAcceptableAt(now) }
    }

    @Test
    fun `원본에서 뽑은 이벤트는 기간 밖을 버리고 시각순으로 놓되 같은 시각은 원래 순서를 지킨다`() {
        val events = AnalysisEvents(
            listOf(
                AnalysisEvent(now.minusSeconds(60), "둘째"),
                AnalysisEvent(now.plusSeconds(301), "미래"),
                AnalysisEvent(now.minusSeconds(120), "첫째"),
                AnalysisEvent(YEAR_START.minusMillis(1), "작년"),
                AnalysisEvent(now.minusSeconds(60), "셋째"),
            ),
        )
        assertEquals(listOf("첫째", "둘째", "셋째"), events.selectedAt(now).toList().map { it.text })
    }

    @Test
    fun `본인 이벤트가 2,000개를 넘으면 달마다 고르게 2,000개를 고르고 각 달 최신부터 남긴다`() {
        val events = AnalysisEvents(monthly(SELF_PER_MONTH) { month, index -> AnalysisEvent(at(month, index), "나$month-$index") })

        val selected = events.selectedAt(now).toList()

        assertEquals(MAX_EVENTS, selected.size)
        assertEquals((1..NOVEMBER).toList(), selected.map { it.occurredAt.atZone(SeoulTime.ZONE).monthValue }.distinct())
        assertEquals("나1-${SELF_PER_MONTH - MAX_EVENTS / NOVEMBER}", selected.first().text)
        assertEquals(selected.sortedBy { it.occurredAt }, selected)
    }

    @Test
    fun `상대 이벤트가 많아도 본인 이벤트는 모두 남기고 상대 이벤트만 전체 6,000개 안으로 고른다`() {
        val selves = List(SELF_COUNT) { index -> AnalysisEvent(now.minus(Duration.ofDays(30)).plusSeconds(index.toLong()), "나$index") }
        val partners = List(MAX_TOTAL_EVENTS * 2) { index -> AnalysisEvent(now.minusSeconds(index.toLong()), "상대$index", "상대") }

        val selected = AnalysisEvents(partners + selves).selectedAt(now)

        assertEquals(MAX_TOTAL_EVENTS, selected.size())
        assertEquals(SELF_COUNT, selected.toList().count { it.isSelf() })
        assertEquals(selected.toList().sortedBy { it.occurredAt }, selected.toList())
        selected.verifyAcceptableAt(now)
    }

    @Test
    fun `상대가 긴 메시지를 많이 보내는 단체방에서도 글자 수 한도가 본인 이벤트를 밀어내지 않는다`() {
        val selves = List(SELF_COUNT) { index -> AnalysisEvent(now.minus(Duration.ofDays(10)).plusSeconds(index.toLong()), "나$index") }
        val partners = List(MAX_TOTAL_EVENTS) { index -> AnalysisEvent(now.minusSeconds(index.toLong()), "가".repeat(100), "상대") }

        val selected = AnalysisEvents(partners + selves).selectedAt(now)

        assertEquals(SELF_COUNT, selected.toList().count { it.isSelf() })
        selected.verifyAcceptableAt(now)
    }

    @Test
    fun `분석 기간은 한국 시각 기준으로 해가 바뀐다`() {
        assertEquals(Instant.parse("2026-12-31T15:00:00Z"), AnalysisEvent.periodStart(Instant.parse("2026-12-31T15:30:00Z")))
        assertEquals(YEAR_START, AnalysisEvent.periodStart(Instant.parse("2026-12-31T14:59:59Z")))
    }

    @Test
    fun `원본에서 뽑은 이벤트는 누적 200,000자까지만 남긴다`() {
        val longText = "가".repeat(2000)
        val events = AnalysisEvents(List(101) { index -> AnalysisEvent(now.minusSeconds(index.toLong()), longText) })
        val selected = events.selectedAt(now)
        assertEquals(100, selected.size())
        assertEquals(now.minusSeconds(99), selected.toList().first().occurredAt)
        selected.verifyAcceptableAt(now)
    }

    @Test
    fun `본인 이벤트가 2,000개를 넘거나 전체 이벤트가 6,000개를 넘으면 TOO_MANY_EVENTS다`() {
        val self = AnalysisEvent(now, "나")
        val partner = AnalysisEvent(now, "상대", "상대")
        AnalysisEvents(List(MAX_EVENTS) { self } + List(MAX_TOTAL_EVENTS - MAX_EVENTS) { partner }).verifyAcceptableAt(now)

        assertErrorCode(ErrorCode.TOO_MANY_EVENTS) { AnalysisEvents(List(MAX_EVENTS + 1) { self }).verifyAcceptableAt(now) }
        assertErrorCode(ErrorCode.TOO_MANY_EVENTS) {
            AnalysisEvents(List(MIN_EVENTS) { self } + List(MAX_TOTAL_EVENTS - MIN_EVENTS + 1) { partner }).verifyAcceptableAt(now)
        }
    }

    /** 1월부터 11월까지 달마다 [count]개씩 만든다. */
    private fun monthly(count: Int, event: (Int, Int) -> AnalysisEvent): List<AnalysisEvent> =
        (1..NOVEMBER).flatMap { month -> List(count) { index -> event(month, index) } }

    /** [month]월 2일 정오(한국 시각)부터 [index]분 뒤. */
    private fun at(month: Int, index: Int): Instant =
        LocalDateTime.of(2026, month, 2, 12, 0).plusMinutes(index.toLong()).atZone(SeoulTime.ZONE).toInstant()

    /** 이벤트 수 하한을 채우도록 정상 이벤트를 뒤에 붙인다. */
    private fun padded(events: List<AnalysisEvent>): AnalysisEvents =
        AnalysisEvents(events + List(MIN_EVENTS - events.size) { AnalysisEvent(now, "채움") })

    private fun assertRejected(events: AnalysisEvents) {
        val exception = assertFailsWith<SantaCutException> { events.verifyAcceptableAt(now) }
        assertEquals(ErrorCode.INVALID_EVENT_RANGE, exception.errorCode)
    }

    companion object {
        private const val SELF_COUNT = 100
        private const val SELF_PER_MONTH = 400
        private const val NOVEMBER = 11
        private val YEAR_START: Instant = Instant.parse("2025-12-31T15:00:00Z")
    }
}
