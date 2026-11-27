package com.santacut.domain.roast.model

import com.santacut.TestDossiers
import com.santacut.TestRoasts
import com.santacut.domain.analysis.model.GiftVerdict
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** 근거 카드 말풍선 표시. 이름은 ○○로 가리고, 전화번호·링크·계좌번호를 지우고, 20자에서 자른다. */
class QuoteTextTest {
    @Test
    fun `전화번호·링크·계좌번호를 지우고 공백을 하나로 줄인다`() {
        assertEquals("내일 여기로 와", QuoteText.render("내일 https://example.com/a 여기로 와", null))
        assertEquals("번호는 이야", QuoteText.render("번호는 010-1234-5678 이야", null))
        assertEquals("입금 부탁", QuoteText.render("입금 110-123-456789 부탁", null))
    }

    @Test
    fun `20자를 넘으면 19자에서 자르고 말줄임표를 붙인다`() {
        val quote = QuoteText.render("가나다라마바사아자차카타파하 가나다라마바사", null)

        assertEquals(QuoteText.MAX_LENGTH, quote.codePointCount(0, quote.length))
        assertTrue(quote.endsWith("…"))
    }

    @Test
    fun `참여자 이름과 호칭으로 부른 이름은 모든 줄의 말풍선에서 ○○로 가린다`() {
        val participants = TestRoasts.catalog.rules.participantNames(listOf("김가람", "이하늘"))
        val messages = listOf(
            TestDossiers.self(TestDossiers.START, "가람아 내일 몇 시?"),
            TestDossiers.self(TestDossiers.START.plusMinutes(1), "하늘이 형도 와?"),
            TestDossiers.self(TestDossiers.START.plusMinutes(2), "보람누나 연락 왔어"),
        )
        val brief = RoastBrief.compose(
            messages, TestDossiers.tally(messages, TestRoasts.dossier).stats, GiftVerdict(TestRoasts.GIFT_SCORE),
            TestRoasts.MIXED, TestRoasts.catalog, TestRoasts.dossier, participants,
        )

        assertEquals("○○아 내일 몇 시?", brief.quoteOf(1))
        listOf(1, 2, 3).forEach { lineId ->
            val quote = brief.quoteOf(lineId).orEmpty()
            listOf("가람", "하늘", "보람").forEach { name -> assertFalse(quote.contains(name), "$lineId: $quote") }
        }
    }
}
