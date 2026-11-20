package com.santacut.domain.roast.model

import com.santacut.TestDossiers
import com.santacut.TestRoasts
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** 통과한 문장 모으기, 다시 쓰기 요청, 대체 문장 채우기. */
class RoastSheetTest {
    private val brief = TestRoasts.brief(
        TestDossiers.selves(List(150) { index -> listOf("내일 ㄱ?", "ㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋ", "언제 볼까?", "ㄱㄱ 바로 출발", "넹")[index % 5] }, minutesApart = 3),
    )

    @Test
    fun `통과한 필드는 지키고 떨어진 필드만 다시 쓰기 요청에 사유와 함께 싣는다`() {
        val first = review(title = "물음표 세 번 요정")
        val sheet = RoastSheet.EMPTY.absorb(first)

        assertEquals(listOf(RoastSheet.TITLE), sheet.missingFields())
        val rewrite = sheet.rewriteFor(first)
        assertEquals(listOf(RoastSheet.TITLE), rewrite.fields)
        assertTrue(rewrite.reasons.any { reason -> reason.startsWith("title 숫자 표현") })
        assertEquals(brief.facts.keys().take(RoastSheet.MAX_PAIRS), rewrite.usedFactKeys)

        val fixed = sheet.absorb(review(title = "단톡방 공식 물음표 자판기", verdict = "다른 판결로 바뀌면 안 되는 문장이다. 그래서 길게 썼다."))
        assertTrue(fixed.isComplete())
        val content = fixed.toContent(brief, TestRoasts.catalog.fallback, 0)
        assertEquals(RoastText("단톡방 공식 물음표 자판기", RoastSource.AI), content.title)
        assertEquals("ㄱ? 치고 판을 깔던 손을 봤다. 선물 명단에 이름 적어 둔다.", content.verdictLine.text)
        assertEquals(0, content.fallbackCount())
    }

    @Test
    fun `읽을 수 없는 응답은 아무것도 채우지 않는다`() {
        val sheet = RoastSheet.EMPTY.absorb(RoastReview.of("not json", brief, TestRoasts.catalog.rules, TestRoasts.mapper))

        assertEquals(6, sheet.missingFields().size)
    }

    @Test
    fun `끝까지 빈 필드는 대체 문장으로 채우고 짝은 사실의 대체 짝과 서버 숫자 칩으로 세 개를 채운다`() {
        val content = RoastSheet.EMPTY.toContent(brief, TestRoasts.catalog.fallback, 7)

        assertEquals(RoastSource.FALLBACK, content.title.source)
        assertEquals("gift", content.verdict)
        assertEquals(RoastSheet.MIN_PAIRS, content.pairs.size())
        content.pairs.toList().forEach { pair ->
            assertEquals(RoastSource.FALLBACK, pair.source)
            assertNotNull(pair.chip)
            assertFalse(pair.roast.contains(PhraseFactSpec.QUOTE))
        }
        assertEquals(listOf(1, 2, 3), content.story.toList().map(RoastStoryLine::act))
    }

    @Test
    fun `사실이 없으면 일반 대체 짝으로 채우고 칩은 없다`() {
        val empty = RoastBrief.compose(emptyList(), null, brief.verdict, TestRoasts.catalog, TestRoasts.dossier)

        val pairs = RoastSheet.EMPTY.toContent(empty, TestRoasts.catalog.fallback, 0).pairs.toList()

        assertEquals(RoastSheet.MIN_PAIRS, pairs.size)
        assertTrue(pairs.all { pair -> pair.factKey == RoastFallback.GENERIC_KEY && pair.chip == null })
    }

    @Test
    fun `사실이 없으면 짝 없이도 나머지 필드가 차면 다시 쓰지 않는다`() {
        val empty = RoastBrief.compose(emptyList(), null, brief.verdict, TestRoasts.catalog, TestRoasts.dossier)
        val noPairs = TestRoasts.mapper.writeValueAsString(
            mapOf(
                "title" to "단톡방 공식 물음표 자판기",
                "verdict" to "먼저 판을 깔던 손을 봤다. 선물 명단에 이름 적어 둔다.",
                "verdict_mild" to "올해 말풍선 잘 읽었다. 선물은 문 앞에 잘 챙겨 두고 간다.",
                "letter" to "올해 네 말풍선을 끝까지 읽었다. 할 말이 생기면 미루지 않고 먼저 꺼내던 버릇이 좋았다. 내년에도 그대로 두어라.",
                "pairs" to emptyList<Any>(),
                "story" to listOf(1, 2, 3).map { act -> mapOf("act" to act, "line" to "그 막에는 먼저 판을 깔자고 졸랐다.") },
            ),
        )
        val answer = RoastReview.of(noPairs, empty, TestRoasts.catalog.rules, TestRoasts.mapper)

        assertTrue(RoastSheet.startFor(empty).absorb(answer).isComplete())
        assertEquals(listOf(RoastPairs.FIELD), RoastSheet.EMPTY.absorb(answer).missingFields())
    }

    private fun review(title: String, verdict: String = "ㄱ? 치고 판을 깔던 손을 봤다. 선물 명단에 이름 적어 둔다."): RoastReview {
        val keys = brief.facts.keys()
        val json = TestRoasts.mapper.writeValueAsString(
            mapOf(
                "title" to title,
                "verdict" to verdict,
                "verdict_mild" to "올해 말풍선 잘 읽었다. 선물은 문 앞에 잘 챙겨 두고 간다.",
                "letter" to "올해 네 말풍선을 끝까지 읽었다. 할 말이 생기면 미루지 않고 먼저 꺼내던 버릇이 좋았다. 내년에도 그대로 두어라.",
                "pairs" to keys.take(RoastSheet.MAX_PAIRS).map { key ->
                    mapOf("fact_key" to key, "roast" to "말풍선 곳곳에 버릇이 묻어난다. 산타 눈에는 다 보인다.", "flip" to "근데 그 버릇 덕에 말풍선마다 이 사람이 또렷하게 보였다.")
                },
                "story" to listOf(1, 2, 3).map { act -> mapOf("act" to act, "line" to "그 막에는 먼저 판을 깔자고 졸랐다.") },
            ),
        )
        return RoastReview.of(json, brief, TestRoasts.catalog.rules, TestRoasts.mapper)
    }
}
