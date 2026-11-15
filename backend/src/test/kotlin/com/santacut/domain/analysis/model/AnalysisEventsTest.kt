package com.santacut.domain.analysis.model

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
        val events = AnalysisEvents(
            listOf(
                AnalysisEvent(now.minus(Duration.ofDays(90)), "가장 오래된 이벤트"),
                AnalysisEvent(now.plusSeconds(300), "가장 늦은 이벤트"),
            ),
        )
        events.verifyAcceptableAt(now)
        assertEquals(2, events.size())
    }

    @Test
    fun `5분 넘게 미래인 이벤트가 있으면 범위 오류다`() {
        assertRejected(AnalysisEvents(listOf(AnalysisEvent(now.plusSeconds(301), "미래 이벤트"))))
    }

    @Test
    fun `90일보다 오래된 이벤트가 있으면 범위 오류다`() {
        val tooOld = now.minus(Duration.ofDays(90)).minusMillis(1)
        assertRejected(AnalysisEvents(listOf(AnalysisEvent(tooOld, "오래된 이벤트"))))
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

    private fun assertRejected(events: AnalysisEvents) {
        val exception = assertFailsWith<SantaCutException> { events.verifyAcceptableAt(now) }
        assertEquals(ErrorCode.INVALID_EVENT_RANGE, exception.errorCode)
    }
}
