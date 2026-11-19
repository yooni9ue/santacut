package com.santacut.domain.dossier.model

import com.santacut.TestDossiers
import com.santacut.TestDossiers.partner
import com.santacut.TestDossiers.self
import com.santacut.TestDossiers.selves
import com.santacut.TestDossiers.tally
import com.santacut.domain.analysis.model.ScoringMessage
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 조서 집계값을 항목마다 확인한다. 본인 메시지만 세고, 상대 메시지는 답장 간격과 묶음 경계에만 쓰는지도 본다. */
class DossierMessagesTest {
    @Test
    fun `메시지 수, 글자 수, 활동일, 가장 바쁜 날은 본인 메시지로 하루 경계 05시로 센다`() {
        val messages = listOf(
            self(at(3, 2, 10), "안녕"), self(at(3, 3, 4), "새벽"), self(at(3, 3, 10), "ㅎ"),
            partner(at(3, 3, 11), "상대가 아주 길게 보낸 말"),
        )

        val volume = tally(messages).stats.volume

        assertEquals(VolumeStats(3, 5, 2, BusiestDay(2, 3)), volume)
    }

    @Test
    fun `ㅋ·ㅎ와 ㅠ·ㅜ 글자 수, 100개당 밀도, 최장 ㅋ 연타와 그 달·시를 센다`() {
        val messages = listOf(self(at(3, 2, 10), "ㅋㅋㅋ"), self(at(8, 10, 23), "ㅋㅋㅋㅋㅋ ㅎㅎ"), self(at(9, 1, 12), "ㅠㅠ"))

        val laugh = tally(messages).stats.laugh

        assertEquals(LaughStats(10, 333.3, LaughRun(5, 8, 23), 2, 66.7), laugh)
    }

    @Test
    fun `이모지는 피부색이 붙어도 하나로 세고 많이 쓴 순서로 셋까지 남긴다`() {
        val messages = selves(listOf("👍🏻👍🏻", "😂", "😂😂 하하"))

        val emoji = tally(messages).stats.emoji

        assertEquals(listOf(EmojiTally("😂", 3), EmojiTally("👍🏻", 2)), emoji.top.toList())
        assertEquals(166.7, emoji.density)
    }

    @Test
    fun `시간대·달·요일 분포와 심야 비율, 가장 붐빈 달과 조용한 달의 배수를 센다`() {
        val messages = List(3) { index -> self(at(1, 10, 1).plusMinutes(index.toLong()), "새벽 $index") } + self(at(2, 10, 12), "낮")

        val clock = tally(messages).stats.clock

        assertEquals(3, clock.hourly[1])
        assertEquals(listOf(3, 1), clock.monthly.take(2))
        assertEquals(3, clock.weekday[FRIDAY])
        assertEquals(1, clock.peakHour)
        assertEquals(0.75, clock.nightRatio)
        assertEquals(Triple(1, 2, 3.0), Triple(clock.busiestMonth, clock.quietestMonth, clock.monthRatio))
    }

    @Test
    fun `1대1 대화에서 상대 말 뒤 10분 넘게 조용했다가 온 본인 첫 답장 간격의 중앙값을 분으로 낸다`() {
        val reply = tally(conversation(samples = 10)).stats.reply

        assertEquals(ReplyStats(true, SLOW_REPLY_MINUTES.toInt()), reply)
    }

    @Test
    fun `표본이 10개보다 적거나 단체방이면 답장 간격은 없다`() {
        assertNull(tally(conversation(samples = 9)).stats.reply.medianMinutes)
        val group = conversation(samples = 10) + partner(at(12, 1, 10), "다른 사람", "상대2")
        assertFalse(tally(group).stats.reply.oneOnOne)
        assertNull(tally(group).stats.reply.medianMinutes)
    }

    @Test
    fun `메시지 모양과 3분 안의 본인 연속 묶음을 센다`() {
        val start = at(3, 2, 10)
        val messages = listOf(
            self(start, "가"), self(start.plusMinutes(1), "가나다라마바"), partner(start.plusMinutes(2), "응"),
            self(start.plusMinutes(3), "뭐?"), self(start.plusMinutes(10), "와!"),
        )

        val style = tally(messages).stats.style

        assertEquals(MessageStyle(2.8, 0.75, 1.3, 2, 0.25, 0.25, 6), style)
    }

    @Test
    fun `입버릇은 허용 어휘만 많이 쓴 순서와 특이도 순서로 고르고 한 글자 말은 낱말일 때만 센다`() {
        val messages = selves(List(10) { "진짜 ㅇㅋ" } + List(5) { "헐 그래" } + listOf("헐랭"))

        val words = tally(messages).stats.words

        assertEquals(listOf("jinjja" to 10, "okay" to 10, "heol" to 5), words.phrasesFrequent.toList().map { it.key to it.count })
        assertEquals(listOf("heol", "okay", "jinjja"), words.phrasesDistinct.toList().map(PhraseTally::key))
    }

    @Test
    fun `따뜻한 말, 막별 주제, 욕설 합산을 센다`() {
        val messages = listOf(
            self(at(3, 2, 10), "고마워 진짜 고맙다"), self(at(3, 3, 10), "밥 먹자 치킨"), self(at(9, 1, 10), "여행 가자 ㅅㅂ"),
        )

        val words = tally(messages).stats.words

        assertEquals(mapOf("thanks" to 2), words.kindWords.toMap())
        assertEquals(mapOf("food" to listOf(2, 0, 0), "travel" to listOf(0, 0, 1)), words.topics.toMap())
        assertEquals(1, words.bleepCount)
    }

    @Test
    fun `위기 표현이 1000개당 기준 이상이면 담백 톤이고 그 횟수는 집계에 남기지 않는다`() {
        val heavy = tally(selves(listOf("우울해", "그래")))
        val normal = tally(selves(listOf("좋아", "그래")))

        assertEquals(DossierTone.MILD, heavy.tone)
        assertEquals(DossierTone.NORMAL, normal.tone)
        assertFalse(TestDossiers.mapper.writeValueAsString(heavy.stats).contains("우울"))
    }

    @Test
    fun `저장용 JSON에는 원문이 들어가지 않고 다시 읽으면 같은 집계다`() {
        val messages = selves(listOf("진짜 비밀 이야기 http://example.com 010-1234-5678", "ㅋㅋ 진짜", "진짜 고마워"))
        val stats = tally(messages).stats

        val json = TestDossiers.mapper.writeValueAsString(stats)

        assertTrue(listOf("비밀", "example", "010-").none(json::contains), json)
        assertEquals(json, TestDossiers.mapper.writeValueAsString(TestDossiers.mapper.readValue(json, DossierStats::class.java)))
    }

    /** 상대 말 뒤 [SLOW_REPLY_MINUTES]분 만에 본인이 답한 표본 [samples]개와, 5분 만에 답해 표본이 아닌 대화 하나. */
    private fun conversation(samples: Int): List<ScoringMessage> =
        List(samples) { index ->
            val asked = at(4, 1, 9).plusHours(index * 2L)
            listOf(partner(asked, "물어봄"), self(asked.plusMinutes(SLOW_REPLY_MINUTES), "답함"))
        }.flatten() + listOf(partner(at(11, 1, 9), "또 물어봄"), self(at(11, 1, 9).plusMinutes(5), "빨리 답함"))

    private fun at(month: Int, day: Int, hour: Int): LocalDateTime = LocalDateTime.of(2026, month, day, hour, 0)

    companion object {
        private const val FRIDAY = 4
        private const val SLOW_REPLY_MINUTES = 20L
    }
}
