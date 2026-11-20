package com.santacut.domain.roast.model

import com.santacut.TestDossiers
import com.santacut.TestRoasts
import com.santacut.domain.analysis.model.GiftVerdict
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 작가 입력 조립. 본인 줄만 가려서 막별로 넣고, 사실은 숫자 없이 수준 말만 넣는다. */
class RoastBriefTest {
    @Test
    fun `입력에는 본인 줄만 가린 채 막별로 들어가고 상대 말은 빠진다`() {
        val messages = habitMessages() + listOf(
            TestDossiers.partner(MARCH.plusMinutes(1), "상대만 아는 비밀"),
            TestDossiers.self(JULY, "서울역에서 7시 ㄱ?"),
            TestDossiers.self(OCTOBER, "정산 끝나고 연락함"),
        )

        val input = TestRoasts.brief(messages).inputText(TestRoasts.mapper)

        assertFalse(input.contains("상대만 아는 비밀"))
        assertTrue(input.contains("정산 끝나고 연락함"))
        assertTrue(input.contains("|07-14 20시|서울역에서 (숫자)시 ㄱ?"))
        assertTrue(input.contains("\"act\":2,\"months\":\"5~8월\""))
    }

    @Test
    fun `가린 사는 곳 이름은 그 요청의 차단 목록이 되고 지명은 차단하지 않는다`() {
        val brief = TestRoasts.brief(habitMessages() + TestDossiers.self(JULY, "한빛아파트 앞 화담숲 가자"))

        assertEquals("한빛아파트", brief.blockedIn("올해는 한빛아파트 타령이었다"))
        assertNull(brief.blockedIn("올해는 화담숲 타령이었다"))
    }

    @Test
    fun `호칭으로 찾은 이름은 호칭 없이 나온 다른 줄에서도 가리고 줄기를 차단 목록에 넣는다`() {
        val brief = TestRoasts.brief(
            habitMessages() + listOf(TestDossiers.self(JULY, "현규형 언제옴"), TestDossiers.self(OCTOBER, "현규 왔냐 현규잘하네")),
        )

        val input = brief.inputText(TestRoasts.mapper)

        assertFalse(input.contains("현규"))
        assertTrue(input.contains("(이름) 왔냐 (이름)잘하네"))
        assertEquals("현규", brief.blockedIn("올해는 현규 타령이었다"))
    }

    @Test
    fun `조사로만 찾은 줄기는 그 자리에서만 가리고 다른 줄이나 차단 목록으로 퍼뜨리지 않는다`() {
        val brief = TestRoasts.brief(habitMessages() + listOf(TestDossiers.self(JULY, "보람이랑 감"), TestDossiers.self(OCTOBER, "보람 있었다")))

        val input = brief.inputText(TestRoasts.mapper)

        assertTrue(input.contains("(이름)이랑 감"))
        assertTrue(input.contains("보람 있었다"))
        assertNull(brief.blockedIn("보람 있는 한 해였다"))
    }

    @Test
    fun `원본 파일의 참여자 이름은 조사가 붙어도 모든 줄에서 가리고 차단 목록에 넣는다`() {
        val messages = habitMessages() + TestDossiers.self(JULY, "성필이 서산풀코스 가능?")
        val participants = TestRoasts.catalog.rules.participantNames(listOf("평친성필", "평친동준", "윤현식"))

        val brief = RoastBrief.compose(
            messages, TestDossiers.tally(messages, TestRoasts.dossier).stats, GiftVerdict(TestRoasts.GIFT_SCORE), TestRoasts.catalog, TestRoasts.dossier,
            participants,
        )

        val input = brief.inputText(TestRoasts.mapper)
        assertFalse(input.contains("성필"))
        assertTrue(input.contains("(이름)이 서산풀코스 가능?"))
        assertEquals("동준", brief.blockedIn("올해 동준 타령"))
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
