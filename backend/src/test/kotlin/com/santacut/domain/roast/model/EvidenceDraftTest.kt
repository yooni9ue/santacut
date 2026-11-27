package com.santacut.domain.roast.model

import com.santacut.TestDossiers
import com.santacut.TestRoasts
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** 근거 카드 하나의 말풍선 검사. 반응·단답 말풍선과 칩 입버릇이 없는 말풍선은 떨어져 다시 쓰기, 그다음 통계 카드로 간다. */
class EvidenceDraftTest {
    private val rules = TestRoasts.catalog.rules
    private val brief = TestRoasts.brief(
        TestDossiers.selves(List(MESSAGES) { index -> TEXTS[index % TEXTS.size] }, minutesApart = 7),
    )

    @Test
    fun `ㅇㅇ 같은 자모 반응과 한글 세 음절 이하 단답은 근거 말풍선으로 떨어진다`() {
        listOf(REACTION, SHORT).forEach { text ->
            val problems = draft(lineIdOf(text), OTHER_CHIP_WORD).problems(brief, rules)

            assertTrue(problems.any { reason -> reason.startsWith("말풍선이 너무 짧다") }, text)
        }
    }

    @Test
    fun `입버릇 칩을 고르면 말풍선에 그 입버릇이 있어야 한다`() {
        val chipKey = phraseChipKey()

        val wrong = EvidenceDraft("reason", lineIdOf(GOOD_NIGHT), chipKey, "수고장인상", "'수고' 발사로 방을 굴렸다.")
        val right = EvidenceDraft("reason", lineIdOf(THANKS), chipKey, "수고장인상", "'수고' 발사로 방을 굴렸다.")

        assertTrue(wrong.problems(brief, rules).contains("말풍선에 칩 입버릇('수고')이 없다"))
        assertFalse(right.problems(brief, rules).any { reason -> reason.contains("입버릇") || reason.contains("말풍선이") })
    }

    @Test
    fun `회귀 - 푹 자 내일 보자에 수고 칩을 붙이거나 살아남은 말로 ㅇㅇ을 고르면 떨어진다`() {
        val mismatched = EvidenceDraft("reason", lineIdOf(GOOD_NIGHT), phraseChipKey(), "수고장인상", "'수고' 발사로 방을 굴렸다.")
        val keepReaction = EvidenceDraft("keep", lineIdOf(REACTION), phraseChipKey(), "수고생존상", "'수고' 발사는 봐준다.")

        assertTrue(mismatched.problems(brief, rules).any { reason -> reason.contains("입버릇('수고')") })
        assertTrue(keepReaction.problems(brief, rules).any { reason -> reason.startsWith("말풍선이 너무 짧다") })
    }

    @Test
    fun `말풍선 없이 칩만 쓴 깎인 말은 말풍선 검사를 하지 않는다`() {
        val draft = EvidenceDraft("cut", null, phraseChipKey(), "수고연발상", "'수고' 연발에 귀가 닳았다.")

        assertFalse(draft.problems(brief, rules).any { reason -> reason.contains("말풍선") })
    }

    @Test
    fun `입버릇은 칩 횟수를 센 사전 표면형으로 찾아 미쳤어·ㅈㅉ·말도안돼도 그 입버릇으로 본다`() {
        val formBrief = formBrief()

        listOf(1 to "michyeotda", 2 to "jinjja", 3 to "maldo_an_dwae").forEach { (lineId, key) ->
            val chipKey = PhraseFactSpec.KEY_PREFIX + key
            assertTrue(formBrief.quoteHasPhrase(chipKey, formBrief.quoteOf(lineId).orEmpty()), key)
            val problems = EvidenceDraft("reason", lineId, chipKey, "입버릇장인상", "입버릇 발사로 방을 굴렸다.").problems(formBrief, rules)
            assertFalse(problems.any { reason -> reason.contains("입버릇(") }, "$key $problems")
        }
        assertFalse(formBrief.quoteHasPhrase(PhraseFactSpec.KEY_PREFIX + "michyeotda", formBrief.quoteOf(PLAIN_LINE).orEmpty()))
        assertTrue(
            EvidenceDraft("reason", PLAIN_LINE, PhraseFactSpec.KEY_PREFIX + "jinjja", "입버릇장인상", "입버릇 발사로 방을 굴렸다.")
                .problems(formBrief, rules).contains("말풍선에 칩 입버릇('진짜')이 없다"),
        )
    }

    /** 입버릇 칩 셋(미쳤다·진짜·말도 안 돼)과 표면형으로만 쓴 본인 줄을 쥔 작가 입력. 입버릇은 실제 사전으로 센다. */
    private fun formBrief(): RoastBrief {
        val displays = mapOf("michyeotda" to "미쳤다", "jinjja" to "진짜", "maldo_an_dwae" to "말도 안 돼")
        val facts = RoastFacts(
            displays.map { (key, display) ->
                RoastFact(PhraseFactSpec.KEY_PREFIX + key, "'$display'라는 말을 쓴 횟수", "남들보다 많음", "habit", RoastChip("'$display' 발사 횟수", "9번"))
            },
        )
        val quotes = LineQuotes(
            listOf(LineQuote(1, "와 이거 완전 미쳤어"), LineQuote(2, "ㅈㅉ 오늘 너무 웃겼다"), LineQuote(3, "그게 말도안돼 진심이야"), LineQuote(PLAIN_LINE, "오늘 저녁 뭐 먹을까")),
        )
        return RoastBrief(
            brief.verdict, facts, RoastQuotes.EMPTY, RoastTopics.EMPTY, RoastLines.EMPTY, BlockedWords.EMPTY,
            lineQuotes = quotes, phrases = TestRoasts.dossier.phrases,
        )
    }

    private fun draft(lineId: Int, line: String) = EvidenceDraft("reason", lineId, brief.facts.keys().first(), "약속발사상", line)

    private fun lineIdOf(text: String): Int = (1..MESSAGES).first { lineId -> brief.quoteOf(lineId) == text }

    private fun phraseChipKey(): String = assertNotNull(brief.facts.keys().find { key -> brief.phraseOf(key) == "수고" }, brief.facts.keys().toString())

    companion object {
        private const val MESSAGES = 120
        private const val PLAIN_LINE = 4
        private const val THANKS = "오늘도 수고 많았어"
        private const val GOOD_NIGHT = "푹 자 내일 보자"
        private const val REACTION = "ㅇㅇ"
        private const val SHORT = "ㅇㅋ 알겠어"
        private const val OTHER_CHIP_WORD = "판은 늘 여기서 열렸다."
        private val TEXTS = listOf(THANKS, GOOD_NIGHT, REACTION, SHORT, "다음 주말에 같이 놀러 가자")
    }
}
