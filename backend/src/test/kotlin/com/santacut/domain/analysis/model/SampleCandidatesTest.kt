package com.santacut.domain.analysis.model

import com.santacut.domain.analysis.model.SampleCandidates.Companion.MAX_SAMPLE_CHARS
import com.santacut.domain.analysis.model.SampleCandidates.Companion.MAX_SAMPLE_MESSAGES
import com.santacut.domain.analysis.model.SampleCandidates.Companion.MAX_SAMPLE_TEXT_LENGTH
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 표본의 순서, 한도, 결정성, 짧은 메시지 거르기, 주별 고르게 뽑기를 확인한다.
 */
class SampleCandidatesTest {
    /** 월요일. 이날부터 6일 뒤까지가 한 주다. */
    private val start = LocalDateTime.of(2026, 8, 31, 9, 0)

    @Test
    fun `표본은 시각순이다`() {
        val sample = pick(listOf(at(10, "셋째 메시지"), at(0, "첫째 메시지"), at(3, "둘째 메시지")))

        assertEquals(listOf("첫째 메시지", "둘째 메시지", "셋째 메시지"), sample.toList().map { it.text })
    }

    @Test
    fun `아주 짧거나 웃음·이모지뿐인 메시지는 표본에 넣지 않는다`() {
        val sample = pick(listOf(at(0, "ㅋㅋㅋㅋ"), at(0, "응"), at(0, "😂"), at(0, "ㅠㅠ"), at(0, "오늘 고마웠어")))

        assertEquals(listOf("오늘 고마웠어"), sample.toList().map { it.text })
    }

    @Test
    fun `메시지 수는 120개를 넘지 않는다`() {
        val sample = pick(List(MAX_SAMPLE_MESSAGES * 3) { at(it / 4L, "메시지 $it") })

        assertEquals(MAX_SAMPLE_MESSAGES, sample.size())
    }

    @Test
    fun `본문 글자 수 합은 6,000자를 넘지 않고 긴 메시지는 200자로 자른다`() {
        val sample = pick(List(MAX_SAMPLE_MESSAGES) { at(it.toLong() / 2, "가".repeat(MAX_SAMPLE_TEXT_LENGTH * 2)) })

        assertEquals(MAX_SAMPLE_CHARS / MAX_SAMPLE_TEXT_LENGTH, sample.size())
        assertTrue(sample.totalLength() <= MAX_SAMPLE_CHARS)
        assertTrue(sample.toList().all { it.length() == MAX_SAMPLE_TEXT_LENGTH })
    }

    @Test
    fun `주마다 고르게 뽑아 메시지가 몰린 주가 있어도 모든 주가 표본에 들어간다`() {
        val crowdedWeek = List(MAX_SAMPLE_MESSAGES * 2) { at(0, "첫 주 메시지 $it") }
        val quietWeeks = List(QUIET_WEEKS.toInt()) { week -> at((week + 1) * DAYS_PER_WEEK, "조용한 주 메시지 $week") }

        val sample = pick(crowdedWeek + quietWeeks)

        val weeks = sample.toList().map { it.weekStart() }.distinct()
        assertEquals(QUIET_WEEKS.toInt() + 1, weeks.size)
        assertEquals(MAX_SAMPLE_MESSAGES, sample.size())
    }

    @Test
    fun `같은 주에서는 항목 낱말이 든 메시지를 먼저 뽑는다`() {
        val plain = List(MAX_SAMPLE_MESSAGES) { at(0, "그냥 메시지 $it") }
        val keywordHits = listOf(at(1, "내일 같이 만나"), at(1, "정말 고마워"))

        val sample = pick(plain + keywordHits).toList().map { it.text }

        assertTrue(sample.containsAll(listOf("내일 같이 만나", "정말 고마워")))
        assertEquals(MAX_SAMPLE_MESSAGES, sample.size)
    }

    @Test
    fun `무작위 없이 입력 순서가 달라도 같은 표본을 고른다`() {
        val messages = List(MAX_SAMPLE_MESSAGES * 2) { at(it % WINDOW_DAYS, "메시지 $it ${if (it % 7 == 0) "고마워" else ""}", it.toLong()) }

        val first = pick(messages).toList()

        assertEquals(first, pick(messages).toList())
        assertEquals(first, pick(messages.reversed()).toList())
    }

    @Test
    fun `뽑을 메시지가 없으면 빈 표본이다`() {
        assertEquals(0, pick(listOf(at(0, "ㅋㅋ"))).size())
        assertEquals(0, pick(emptyList()).size())
    }

    private fun pick(messages: List<ScoringMessage>): SampleMessages = SampleCandidates.from(messages).pick()

    /** 시작 시각에서 [day]일과 [minute]분 뒤의 메시지. */
    private fun at(day: Long, text: String, minute: Long = 0): ScoringMessage =
        ScoringMessage(start.plusDays(day).plusMinutes(minute), text)

    companion object {
        private const val QUIET_WEEKS = 12L
        private const val DAYS_PER_WEEK = 7L
        private const val WINDOW_DAYS = 90L
    }
}
