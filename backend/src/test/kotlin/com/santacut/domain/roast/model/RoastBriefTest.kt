package com.santacut.domain.roast.model

import com.santacut.TestDossiers
import com.santacut.TestRoasts
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** 작가 입력 조립. 본인 줄만 가려서 막별로 넣고, 사실은 숫자 없이 수준 말만 넣는다. */
class RoastBriefTest {
    @Test
    fun `입력에는 본인 줄만 가린 채 막별로 들어가고 상대 말과 민감한 줄은 빠진다`() {
        val messages = habitMessages() + listOf(
            TestDossiers.partner(MARCH.plusMinutes(1), "상대만 아는 비밀"),
            TestDossiers.self(JULY, "서울역에서 7시 ㄱ?"),
            TestDossiers.self(OCTOBER, "회식 끝나고 연락함"),
        )

        val input = TestRoasts.brief(messages).inputText(TestRoasts.mapper)

        assertFalse(input.contains("상대만 아는 비밀"))
        assertFalse(input.contains("서울역"))
        assertFalse(input.contains("회식"))
        assertTrue(input.contains("|07-14 20시|(장소)에서 (숫자)시 ㄱ?"))
        assertTrue(input.contains("\"act\":2,\"months\":\"5~8월\""))
    }

    @Test
    fun `가린 고유명사는 그 요청의 차단 목록이 되어 출력에서 걸린다`() {
        val brief = TestRoasts.brief(habitMessages() + TestDossiers.self(JULY, "화담숲 가자"))

        assertEquals("화담숲", brief.blockedIn("올해는 화담숲 타령이었다"))
    }

    @Test
    fun `사실에는 숫자 값 없이 뜻·수준·계열만 들어가고 인용 후보는 사전 원형 표기다`() {
        val brief = TestRoasts.brief(habitMessages())
        val input = TestRoasts.mapper.readTree(brief.inputText(TestRoasts.mapper))

        val facts = input.path("facts").values()
        assertTrue(facts.isNotEmpty())
        facts.forEach { fact -> assertEquals(setOf("key", "meaning", "level", "group"), fact.propertyNames().toSet()) }
        assertTrue(facts.map { fact -> fact.path("key").asString() }.contains("phrase_giyeok_q"))
        val quotable = input.path("quotable").values().map { quote -> quote.asString() }
        assertTrue(quotable.contains("ㄱ?"))
        assertFalse(quotable.contains("조지자"))
        assertFalse(quotable.contains("행님들"))
        assertEquals("gift", input.path("verdict").asString())
        assertEquals("넉넉", input.path("margin").asString())
    }

    @Test
    fun `세 막의 1위 화제가 같으면 2위 화제도 후보로 넘긴다`() {
        val messages = (
            List(120) { index -> TestDossiers.self(MARCH.plusMonths((index % 3 * 4).toLong()).plusMinutes(index.toLong()), "언제 볼까 약속 ㄱ?") } +
                TestDossiers.selves(List(10) { "배고파 밥 먹자" })
            ).sortedBy { message -> message.localTime }
        val topics = TestRoasts.mapper.readTree(TestRoasts.brief(messages).inputText(TestRoasts.mapper)).path("topics").values()

        val first = assertNotNull(topics.firstOrNull()).path("candidates").values().map { candidate -> candidate.asString() }
        assertEquals(2, first.size)
    }

    /** 버릇이 뚜렷한 본인 메시지 150개(3월). 'ㄱ?'와 'ㅇㅋㅇㅋ', 비속어 버릇을 섞는다. */
    private fun habitMessages() = TestDossiers.selves(
        List(150) { index ->
            when (index % 5) {
                0 -> "내일 ㄱ?"
                1 -> "ㅇㅋㅇㅋ 좋아 ㅋㅋㅋㅋㅋㅋㅋㅋ"
                2 -> "조지자 행님들"
                3 -> "언제 볼까?"
                else -> "ㄱㄱ 바로 출발"
            }
        },
        minutesApart = 3,
    )

    companion object {
        private val MARCH: LocalDateTime = TestDossiers.START
        private val JULY: LocalDateTime = LocalDateTime.of(2026, 7, 14, 20, 5)
        private val OCTOBER: LocalDateTime = LocalDateTime.of(2026, 10, 2, 22, 0)
    }
}
