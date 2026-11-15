package com.santacut.domain.analysis.model

import com.santacut.ErrorCodeAssertions.assertErrorCode
import com.santacut.domain.analysis.model.AnalysisEvents.Companion.MAX_EVENTS
import com.santacut.domain.analysis.model.AnalysisEvents.Companion.MIN_EVENTS
import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AnalysisEventsTest {
    private val now = Instant.parse("2026-11-15T12:00:00Z")

    @Test
    fun `최근 90일 안이고 5분 뒤까지의 이벤트는 받아들인다`() {
        val events = padded(
            listOf(
                AnalysisEvent(now.minus(Duration.ofDays(90)), "가장 오래된 이벤트"),
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
    fun `90일보다 오래된 이벤트가 있으면 범위 오류다`() {
        val tooOld = now.minus(Duration.ofDays(90)).minusMillis(1)
        assertRejected(padded(listOf(AnalysisEvent(tooOld, "오래된 이벤트"))))
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
    fun `이벤트마다 제출 순서대로 1부터 번호를 붙인다`() {
        val events = AnalysisEvents(listOf(AnalysisEvent(now, "첫째"), AnalysisEvent(now, "둘째")))
        assertEquals(listOf("1:첫째", "2:둘째"), events.mapNumbered { number, event -> "$number:${event.text}" })
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
                AnalysisEvent(now.minus(Duration.ofDays(91)), "오래됨"),
                AnalysisEvent(now.minusSeconds(60), "셋째"),
            ),
        )
        assertEquals(listOf("첫째", "둘째", "셋째"), events.recentAt(now).toList().map { it.text })
    }

    @Test
    fun `원본에서 뽑은 이벤트는 최신 2,000개만 남긴다`() {
        val events = AnalysisEvents(List(MAX_EVENTS + 1) { index -> AnalysisEvent(now.minusSeconds(index.toLong()), "e$index") })
        val recent = events.recentAt(now).toList()
        assertEquals(MAX_EVENTS, recent.size)
        assertEquals("e${MAX_EVENTS - 1}", recent.first().text)
        assertEquals("e0", recent.last().text)
    }

    @Test
    fun `원본에서 뽑은 이벤트는 최신부터 누적 200,000자까지만 남긴다`() {
        val longText = "가".repeat(2000)
        val events = AnalysisEvents(List(101) { index -> AnalysisEvent(now.minusSeconds(index.toLong()), longText) })
        val recent = events.recentAt(now)
        assertEquals(100, recent.size())
        assertEquals(now.minusSeconds(99), recent.toList().first().occurredAt)
        recent.verifyAcceptableAt(now)
    }

    /** 이벤트 수 하한을 채우도록 정상 이벤트를 뒤에 붙인다. */
    private fun padded(events: List<AnalysisEvent>): AnalysisEvents =
        AnalysisEvents(events + List(MIN_EVENTS - events.size) { AnalysisEvent(now, "채움") })

    private fun assertRejected(events: AnalysisEvents) {
        val exception = assertFailsWith<SantaCutException> { events.verifyAcceptableAt(now) }
        assertEquals(ErrorCode.INVALID_EVENT_RANGE, exception.errorCode)
    }
}
