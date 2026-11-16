package com.santacut.domain.analysis.model

import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 본인 메시지 전체에서 계산하는 활동 통계와 항목별 낱말 신호를 확인한다.
 */
class ScoringMessagesTest {
    private val start = LocalDateTime.of(2026, 11, 1, 0, 0)

    @Test
    fun `메시지 수, 보낸 날 수, 하루 평균, 가장 긴 연속 일수를 센다`() {
        val features = ScoringMessages(
            listOf(at(0, 9, "a"), at(0, 10, "b"), at(1, 9, "c"), at(2, 9, "d"), at(5, 9, "e"), at(5, 9, "f"), at(5, 9, "g")),
        ).toFeatures()

        assertEquals(7, features.eventCount)
        assertEquals(4, features.activeDays)
        assertEquals(BigDecimal("1.8"), features.messagesPerActiveDay)
        assertEquals(3, features.longestStreakDays)
    }

    @Test
    fun `시간 구간마다 메시지 수를 센다`() {
        val hours = ScoringMessages(listOf(at(0, 1, "a"), at(0, 7, "b"), at(0, 8, "c"), at(0, 23, "d"))).toFeatures().hourCounts

        assertEquals(listOf(1, 2, 0, 1), HourBucket.entries.map { hours.countOf(it) })
    }

    @Test
    fun `웃음·이모지 비율은 소수 둘째 자리까지다`() {
        val features = ScoringMessages(listOf(at(0, 9, "ㅋㅋㅋ"), at(0, 9, "좋아😊"), at(0, 9, "응"))).toFeatures()

        assertEquals(BigDecimal("0.67"), features.laughEmojiRatio)
    }

    @Test
    fun `항목마다 낱말이 든 메시지 수, 그런 주 수, 전체 주 수, 표본에서 낱말이 든 줄 수를 센다`() {
        val messages = ScoringMessages(
            listOf(
                at(0, 9, "고마워 정말 고마워"),
                at(0, 9, "미안 내일 같이 만나"),
                at(1, 9, "괜찮아? 걱정돼"),
                at(8, 9, "어제 고마웠어"),
                at(15, 9, "밥 먹었어"),
            ),
        )
        val sample = ScoringSample(listOf(at(0, 9, "고마워 정말 고마워")))

        val signals = messages.signals(sample).toList()

        assertEquals(ScoreDimension.entries.toList(), signals.map { it.dimension })
        assertEquals(DimensionSignal(ScoreDimension.POSITIVITY_SUPPORT, 2, 2, 4, 1), signals[1])
        assertEquals(DimensionSignal(ScoreDimension.CARE_EMPATHY, 1, 1, 4, 0), signals[0])
        assertEquals(listOf(1, 2, 1, 1), signals.map { it.hits })
    }

    @Test
    fun `낱말은 어간이라 활용형도 센다`() {
        assertTrue(ScoreTheme.CHEER.isMentionedIn("고마웠어"))
        assertTrue(ScoreTheme.CARE.isMentionedIn("아프지 마"))
        assertTrue(ScoreTheme.TOGETHER.isMentionedIn("언제 만날까"))
    }

    @Test
    fun `메시지가 없으면 비율은 0이다`() {
        val features = ScoringMessages(emptyList()).toFeatures()

        assertEquals(0, features.eventCount)
        assertEquals(BigDecimal("0.0"), features.messagesPerActiveDay)
        assertEquals(BigDecimal("0.00"), features.laughEmojiRatio)
    }

    private fun at(day: Long, hour: Int, text: String): ScoringMessage =
        ScoringMessage(start.plusDays(day).withHour(hour), text)
}
