package com.santacut.domain.analysis.model

import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 본인 메시지 전체에서 세는 메시지 수와 항목별 낱말 신호를 확인한다.
 */
class ScoringMessagesTest {
    private val start = LocalDateTime.of(2026, 11, 1, 0, 0)
    private val messages = ScoringMessages(
        listOf(
            at(0, "고마워 정말 고마워"),
            at(0, "미안 내일 같이 만나"),
            at(1, "괜찮아? 걱정돼"),
            at(8, "어제 고마웠어"),
            at(15, "밥 먹었어"),
        ),
    )
    private val signals = messages.signals(SampleMessages(listOf(at(0, "고마워 정말 고마워")))).toList()

    @Test
    fun `메시지 수를 센다`() {
        assertEquals(5, messages.size())
        assertEquals(0, ScoringMessages(emptyList()).size())
    }

    @Test
    fun `신호는 항목 순서대로 하나씩 만든다`() {
        assertEquals(ScoreDimension.entries.toList(), signals.map { signal -> signal.dimension })
        assertEquals(listOf(1, 2, 1, 1), signals.map { signal -> signal.hits })
    }

    @Test
    fun `항목마다 낱말이 든 메시지 수, 그런 주 수, 전체 주 수, 표본에서 낱말이 든 줄 수를 센다`() {
        assertEquals(DimensionSignal(ScoreDimension.POSITIVITY_SUPPORT, 2, 2, 4, 1), signals[1])
        assertEquals(DimensionSignal(ScoreDimension.CARE_EMPATHY, 1, 1, 4, 0), signals[0])
    }

    @Test
    fun `낱말은 어간이라 활용형도 센다`() {
        assertTrue(ScoreTheme.CHEER.isMentionedIn("고마웠어"))
        assertTrue(ScoreTheme.CARE.isMentionedIn("아프지 마"))
        assertTrue(ScoreTheme.TOGETHER.isMentionedIn("언제 만날까"))
    }

    private fun at(day: Long, text: String): ScoringMessage = ScoringMessage(start.plusDays(day).withHour(NINE), text)

    companion object {
        private const val NINE = 9
    }
}
