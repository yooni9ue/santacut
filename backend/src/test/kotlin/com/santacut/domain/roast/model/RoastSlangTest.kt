package com.santacut.domain.roast.model

import com.santacut.TestDossiers
import com.santacut.TestRoasts
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 유행어 목록(roast/slang.json): 허용 표기는 통과하고 필드마다 하나까지, 금지 말은 막는다. */
class RoastSlangTest {
    private val rules = TestRoasts.catalog.rules
    private val gift = TestRoasts.briefWithoutPunches(TestDossiers.selves(List(MESSAGES) { "ㄱㄱ 바로 가자" }))

    @Test
    fun `유행어 목록은 갱신 날짜와 출처를 지니고 규칙 위반이 없다`() {
        assertEquals("2026-11-27", rules.slang.updated)
        assertTrue(rules.slang.sources.isNotEmpty())
        assertEquals(emptyList(), rules.slang.violations())
    }

    @Test
    fun `허용 유행어는 물결표·말줄임표·알파벳 표기 그대로 검증을 통과한다`() {
        listOf(
            "약속 잡히자마자 티~원 외친다.",
            "요이~ 소리에 말풍선 폭주 시작.",
            "엄… 물음표만 보내 놓고 기다린다.",
            "답장 속도 대결은 L을 가져가.",
            "답 안 할 거면 넌 나가라.",
            "약속은 늘 네가 잡았다. 넌 남아라.",
            "새벽 말풍선 현행범, 체포하겠어.",
        ).forEach { text -> assertEquals(emptyList(), TextProblems.of(text, RoastTextKind.FINAL_LINE, gift, rules).toList(), text) }
    }

    @Test
    fun `한 필드에 유행어를 둘 넘게 쓰면 떨어진다`() {
        val problems = TextProblems.of("요이~ 하고 개야르로 끝난다.", RoastTextKind.FINAL_LINE, gift, rules).toList()

        assertTrue(problems.any { reason -> reason.startsWith("유행어는 필드마다 하나까지") }, problems.toString())
        assertNull(rules.slangOverflowIn("개야르 하고 끝나는 말풍선이다."))
    }

    @Test
    fun `금지 유행어와 실존 인물 이름 꼴은 금지어로 막는다`() {
        listOf("쥐~랄", "밤티", "영포티", "테토남", "에겐", "멘헤라", "허버허버", "장원영", "한남 말투", "가난하게 컸어", "샤갈")
            .forEach { word -> assertNotNull(rules.bannedIn("이 버릇은 완전 $word 같다."), word) }
    }

    @Test
    fun `영포티 말투는 금지어로 막고 허용 목록에서 뺐다`() {
        listOf("단톡방 엘프", "약속의 달인", "답장 잘하는 편", "판을 늘 시작하는 자", "MZ 감성", "갓생러", "킹받는다", "오히려 좋아")
            .forEach { text -> assertNotNull(rules.bannedIn(text), text) }
        assertTrue(rules.slang.allowed.toList().none { word -> word.word == "킹받다" })
    }

    @Test
    fun `덱 전체 유행어 상한은 세 개다`() {
        assertEquals(3, rules.slang.maxPerDeck)
        assertEquals(2, rules.slangCountIn("요이~ 하고 딸깍."))
    }

    @Test
    fun `금지 유행어 경계는 흔한 말을 잡지 않는다`() {
        listOf("너에겐 그 말풍선이 전부다.", "한남동 가자고 졸랐다.", "테토스 게임 얘기만 한다.")
            .forEach { text -> assertNull(rules.bannedIn(text), text) }
    }

    @Test
    fun `목록 밖 알파벳은 여전히 막고 느낌표도 여전히 막는다`() {
        assertTrue(rules.hasLatin("답장 속도는 GG다."))
        assertNotNull(rules.toneIn("티~원 외친다!"))
    }

    @Test
    fun `작가 입력에는 유행어 표기와 뜻만 실린다`() {
        val slang = TestRoasts.mapper.readTree(gift.inputText(TestRoasts.mapper)).path("slang")

        assertTrue(slang.has("줴줴이야"))
        assertTrue(slang.has("티~원"))
        assertTrue(slang.path("줴줴이야").asString().isNotBlank())
    }

    companion object {
        private const val MESSAGES = 60
    }
}
