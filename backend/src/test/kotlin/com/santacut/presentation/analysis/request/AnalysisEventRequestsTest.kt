package com.santacut.presentation.analysis.request

import com.santacut.domain.analysis.model.AnalysisEvent
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnalysisEventRequestsTest {
    private val first = Instant.parse("2026-12-01T08:30:00Z")
    private val second = Instant.parse("2026-12-01T09:00:00Z")

    @Test
    fun `요청 이벤트를 제출 순서 그대로 같은 시각과 본문의 명령으로 바꾼다`() {
        val requests = AnalysisEventRequests(
            listOf(AnalysisEventRequest(second, "늦은 시각 이벤트"), AnalysisEventRequest(first, "이른 시각 이벤트")),
        )

        val events = requests.toCommands().toEvents().toList()

        assertEquals(
            listOf(AnalysisEvent(second, "늦은 시각 이벤트"), AnalysisEvent(first, "이른 시각 이벤트")),
            events,
        )
    }

    @Test
    fun `빈 요청 목록은 빈 명령 목록이 된다`() {
        assertTrue(AnalysisEventRequests(emptyList()).toCommands().toEvents().toList().isEmpty())
    }
}
