package com.santacut.domain.analysis.model

import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScoringMessageTest {
    private val noon = LocalDateTime.of(2026, 11, 14, 12, 0)

    @Test
    fun `웃음, 울음, 기호, 이모지, 공백을 빼고 2자보다 짧으면 표본 가치가 낮다`() {
        listOf("ㅋㅋㅋㅋ", "ㅎㅎ", "ㅠㅠ", "응", "ㅇ", "!!", "😂😂", "👍🏻", "❤️", "  ", "응ㅋㅋ").forEach {
            assertTrue(message(it).isLowSignal(), it)
        }
        listOf("고마워", "ㅋㅋ 진짜?", "ok", "잘자😊").forEach { assertFalse(message(it).isLowSignal(), it) }
    }

    @Test
    fun `웃음이나 울음이 두 글자 이상 이어지거나 이모지가 있으면 웃음·이모지 메시지다`() {
        listOf("좋아ㅋㅋ", "헐ㅎㅎ", "슬퍼ㅠㅠ", "최고👍").forEach { assertTrue(message(it).hasLaughOrEmoji(), it) }
        listOf("고마워", "ㅋ", "ㅠ 알겠어").forEach { assertFalse(message(it).hasLaughOrEmoji(), it) }
    }

    @Test
    fun `주는 월요일에 시작한다`() {
        val monday = LocalDate.of(2026, 11, 9)

        assertEquals(monday, ScoringMessage(LocalDateTime.of(2026, 11, 9, 0, 0), "a").weekStart())
        assertEquals(monday, ScoringMessage(LocalDateTime.of(2026, 11, 15, 23, 59), "a").weekStart())
        assertEquals(monday.plusWeeks(1), ScoringMessage(LocalDateTime.of(2026, 11, 16, 0, 0), "a").weekStart())
    }

    @Test
    fun `시간 구간은 6시간 단위다`() {
        assertEquals(HourBucket.NIGHT, ScoringMessage(noon.withHour(5), "a").hourBucket())
        assertEquals(HourBucket.MORNING, ScoringMessage(noon.withHour(6), "a").hourBucket())
        assertEquals(HourBucket.AFTERNOON, ScoringMessage(noon.withHour(17), "a").hourBucket())
        assertEquals(HourBucket.EVENING, ScoringMessage(noon.withHour(23), "a").hourBucket())
    }

    @Test
    fun `자른 메시지는 최대 길이를 넘지 않는다`() {
        assertEquals("가나", message("가나다라").truncated(2).text)
        assertEquals("가나", message("가나").truncated(5).text)
    }

    private fun message(text: String): ScoringMessage = ScoringMessage(noon, text)
}
