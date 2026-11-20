package com.santacut.domain.roast.model

import com.santacut.TestDossiers
import com.santacut.TestRoasts
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 작가 응답 필드 단위 검증. 떨어진 필드마다 사유를 남긴다. */
class RoastReviewTest {
    private val gift = TestRoasts.brief(messages())
    private val coal = TestRoasts.brief(messages(), TestRoasts.COAL_SCORE)
    private val factKeys = gift.facts.keys()

    @Test
    fun `규칙을 지킨 응답은 모든 필드가 통과한다`() {
        val review = review(gift, validJson())

        assertEquals(emptyList(), review.checks.failedFields())
    }

    @Test
    fun `숫자·금지어·목록 밖 인용·영문·괄호는 그 필드만 떨어진다`() {
        val json = validJson(
            title = "물음표 세 번 요정",
            verdict = "상대가 답할 때까지 물음표를 보냈다. 올해 선물 명단에 올린다.",
            letter = "올해 '언제 볼까'로 판을 열던 버릇이 좋았다. Santa도 그 버릇은 그대로 두라고 한다. 내년에도 그렇게 먼저 말을 꺼내라.",
            verdictMild = "올해 말풍선 잘 읽었다. (장소)에서 내년에 선물을 챙겨 보낸다.",
        )

        val checks = review(gift, json).checks

        assertEquals(listOf("title", "verdict", "verdict_mild", "letter"), checks.failedFields())
        val reasons = checks.reasonsFor(listOf("title", "verdict", "verdict_mild", "letter"))
        assertTrue(reasons.any { reason -> reason.contains("숫자 표현(세 번)") })
        assertTrue(reasons.any { reason -> reason.contains("금지어(상대)") })
        assertTrue(reasons.any { reason -> reason.contains("목록 밖 인용(언제 볼까)") })
        assertTrue(reasons.any { reason -> reason.contains("알파벳") })
        assertTrue(reasons.any { reason -> reason.contains("괄호") })
    }

    @Test
    fun `돈·직장 낱말과 다들은 금지어가 아니라 통과한다`() {
        val json = validJson(verdict = "연차 내고 정산까지 챙기던 손을 봤다. 다들 모인 판에 선물 얹는다.")

        assertEquals(emptyList(), review(gift, json).checks.failedFields())
    }

    @Test
    fun `본인 말풍선에 있는 웃음과 울음 자모 인용은 목록에 없어도 통과한다`() {
        val brief = TestRoasts.brief(messages() + TestDossiers.self(TestDossiers.START.plusDays(200), "아 진짜 ㅋㅋㅋㅋ 그건 아니지 ㅠㅠ"))
        val laugh = validJson(brief.facts.keys(), verdict = "'ㅋㅋㅋ' 치고 바로 다음 말을 꺼내던 손을 봤다. 선물 명단에 올린다.")
        val cry = validJson(brief.facts.keys(), verdict = "'ㅠㅠ' 달고도 끝까지 일정을 묻던 손을 봤다. 선물 명단에 올린다.")

        assertEquals(emptyList(), review(brief, laugh).checks.failedFields())
        assertEquals(emptyList(), review(brief, cry).checks.failedFields())
    }

    @Test
    fun `말풍선에 없는 웃음 인용과 자모 비속어 인용은 떨어진다`() {
        val laugh = validJson(verdict = "'ㅎㅎㅎ' 붙여 넘기던 손을 봤다. 선물 명단에 올린다.")
        val curse = validJson(verdict = "'ㅅㅂ' 대신 다른 말을 고르던 손을 봤다. 선물 명단에 올린다.")

        assertTrue(review(gift, laugh).checks.reasonsFor(listOf("verdict")).any { reason -> reason.contains("목록 밖 인용(ㅎㅎㅎ)") })
        assertTrue(review(gift, curse).checks.reasonsFor(listOf("verdict")).any { reason -> reason.contains("목록 밖 인용(ㅅㅂ)") })
    }

    @Test
    fun `큰따옴표와 낫표로 감싼 목록 밖 인용도 떨어진다`() {
        val json = validJson(
            title = "“언제 볼까” 신호탄 요정",
            verdict = "「언제 볼까?」 하나로 판을 깔던 손을 봤다. 선물 명단에 올린다.",
            letter = "올해 『언제 볼까』로 판을 열던 버릇이 좋았다. 할 말이 생기면 미루지 않고 먼저 꺼냈다. 내년에도 그대로 둬라.",
        )

        val checks = review(gift, json).checks

        assertEquals(listOf("title", "verdict", "letter"), checks.failedFields())
        assertEquals(3, checks.reasonsFor(listOf("title", "verdict", "letter")).count { reason -> reason.contains("목록 밖 인용(언제 볼까") })
    }

    @Test
    fun `따옴표 없이 본인 말풍선을 여덟 글자 넘게 베끼면 떨어진다`() {
        val brief = TestRoasts.brief(messages() + TestDossiers.self(TestDossiers.START.plusDays(200), "오늘은 무조건 보드게임 하자"))
        val json = validJson(brief.facts.keys(), verdict = "무조건 보드게임 하자고 조르던 손을 봤다. 선물 명단에 올린다.")

        val checks = review(brief, json).checks

        assertEquals(listOf("verdict"), checks.failedFields())
        assertTrue(checks.reasonsFor(listOf("verdict")).any { reason -> reason.contains("말풍선 베낌") })
    }

    @Test
    fun `석탄인데 선물을 받는다는 판결과 석탄을 쓴 담백 판결, 근데로 열지 않은 뒤집기는 떨어진다`() {
        val json = validJson(
            verdict = "ㄱ? 하나로 버텼다. 그래도 올해 선물 받는 건 어렵지 않겠다.",
            verdictMild = "올해 말풍선 잘 읽었다. 석탄은 접어 두고 내년에 다시 보자.",
            firstFlip = "그 덕에 판이 늘 먼저 열렸다. 그건 고마운 일이다.",
        )

        val failed = review(coal, json).checks.failedFields()

        assertTrue(failed.containsAll(listOf("verdict", "verdict_mild", "pairs[0]")))
    }

    @Test
    fun `옛말투 어미·유행어·느낌표·ㅋㅋ·이모지는 그 필드만 떨어진다`() {
        val json = validJson(
            title = "단톡방 레전드 자판기",
            verdict = "말풍선을 끝까지 읽었구나. 선물 명단에 이름 적어 둔다.",
            verdictMild = "올해 말풍선 잘 읽었습니다. 선물은 문 앞에 두고 간다.",
            firstFlip = "근데 그 손 없었으면 이 방은 진작 얼었다$SNOWFLAKE.",
            letter = "올해 네 말풍선을 끝까지 읽었다. 할 말이 생기면 미루지 않고 먼저 꺼내더라! 내년에도 먼저 말 꺼내거라.",
        )

        val checks = review(gift, json).checks

        assertEquals(listOf("title", "verdict", "verdict_mild", "letter", "pairs[0]"), checks.failedFields())
        val reasons = checks.reasonsFor(listOf("title", "verdict", "verdict_mild", "letter", "pairs[0]"))
        listOf("레전드", "구나", "습니다", "!", SNOWFLAKE).forEach { found ->
            assertTrue(reasons.any { reason -> reason.contains("금지 말투($found)") }, found)
        }
    }

    @Test
    fun `칭호에는 요정·천사·분위기 메이커를 쓸 수 없고 다른 필드에는 막지 않는다`() {
        listOf("물음표 신호탄 요정", "단톡방 수호 천사", "단톡방 분위기 메이커").forEach { title ->
            val reasons = review(gift, validJson(title = title)).checks.reasonsFor(listOf("title"))
            assertTrue(reasons.any { reason -> reason.contains("칭호 금지어") }, title)
        }
        val verdict = validJson(verdict = "요정처럼 판을 깔던 손을 봤다. 선물 명단에 이름 적어 둔다.")
        assertEquals(emptyList(), review(gift, verdict).checks.failedFields())
    }

    @Test
    fun `쪽지 마지막 문장만 거라로 맺을 수 있고 따옴표 인용 안의 ㅋㅋ는 말투 검사에서 뺀다`() {
        val brief = TestRoasts.brief(messages() + TestDossiers.self(TestDossiers.START.plusDays(200), "아 진짜 ㅋㅋㅋ 그건 아니지"))
        val closing = validJson(brief.facts.keys(), letter = "올해 네 말풍선을 끝까지 읽었다. 할 말이 생기면 미루지 않고 먼저 꺼내더라. 솔직히 좀 부러웠다. 내년에도 먼저 말 꺼내거라.")
        val middle = validJson(brief.facts.keys(), letter = "올해 네 말풍선을 끝까지 읽었다. 먼저 말을 하거라. 그 버릇이 이 방을 깨웠다. 솔직히 좀 부러웠다. 내년에도 그대로 두마.")
        val quoted = validJson(brief.facts.keys(), verdict = "'ㅋㅋㅋ' 치고 바로 다음 말을 꺼내던 손을 봤다. 선물 명단에 올린다.")
        val bare = validJson(brief.facts.keys(), verdict = "ㅋㅋ 치고 바로 다음 말을 꺼내던 손을 봤다. 선물 명단에 올린다.")

        assertEquals(emptyList(), review(brief, closing).checks.failedFields())
        assertEquals(emptyList(), review(brief, quoted).checks.failedFields())
        assertTrue(review(brief, middle).checks.reasonsFor(listOf("letter")).any { reason -> reason.contains("금지 말투(하거라)") })
        assertTrue(review(brief, bare).checks.reasonsFor(listOf("verdict")).any { reason -> reason.contains("금지 말투(ㅋㅋ)") })
    }

    @Test
    fun `근데로 연 뒤집기는 놀림보다 세 자 긴 상한까지 통과한다`() {
        val flip = "근데 " + "가".repeat(RoastTextKind.FLIP.maxLength - 4) + "."

        assertEquals(RoastTextKind.ROAST.maxLength + 3, RoastTextKind.FLIP.maxLength)
        assertEquals(emptyList(), review(gift, validJson(firstFlip = flip)).checks.failedFields())
        assertTrue(review(gift, validJson(firstFlip = flip + "가")).checks.reasonsFor(listOf("pairs[0]")).any { reason -> reason.contains("글자 수") })
    }

    @Test
    fun `없는 사실 key와 앞 짝과 겹친 사실 key는 그 짝만 떨어진다`() {
        val json = validJson(secondKey = "unknown_fact", thirdKey = factKeys[0])

        val failed = review(gift, json).checks.failedFields()

        assertEquals(listOf("pairs[1]", "pairs[2]"), failed)
    }

    @Test
    fun `JSON이 아니면 json 실패 하나만 남고 읽은 값이 없다`() {
        val review = review(gift, "{\"title\": \"잘린 응")

        assertNull(review.output)
        assertEquals(listOf("json"), review.checks.failedFields())
    }

    @Test
    fun `대체 문장은 판정마다 모두 검증 규칙을 통과하고 짝은 선물·석탄과 네 글자 인용에서도 통과한다`() {
        val fallback = TestRoasts.catalog.fallback
        val texts = listOf(
            RoastTextKind.TITLE to fallback.titles, RoastTextKind.VERDICT to fallback.verdicts,
            RoastTextKind.VERDICT_MILD to fallback.verdictsMild, RoastTextKind.LETTER to fallback.letters,
        )
        texts.forEach { (kind, lines) ->
            lines.gift.forEach { text -> assertEquals(emptyList(), TextProblems.of(text, kind, gift, TestRoasts.catalog.rules).toList(), text) }
            lines.coal.forEach { text -> assertEquals(emptyList(), TextProblems.of(text, kind, coal, TestRoasts.catalog.rules).toList(), text) }
        }
        val habit = List(HABIT_LINES) { day -> TestDossiers.self(TestDossiers.START.plusDays(day.toLong()), "$FOUR_CHAR_QUOTE 언제 봄") }
        val giftWithQuote = TestRoasts.brief(messages() + habit)
        val coalWithQuote = TestRoasts.brief(messages() + habit, TestRoasts.COAL_SCORE)
        assertTrue(giftWithQuote.isQuotable(FOUR_CHAR_QUOTE), "네 글자 입버릇이 인용 후보에 있어야 한다")
        val quotes = listOf("ㄱ?", FOUR_CHAR_QUOTE)
        val pairs = quotes.flatMap { quote -> fallback.pairs.values.map { pair -> pair.toPair("x", quote) } } +
            fallback.genericPairs.take(RoastSheet.MIN_PAIRS)
        listOf(giftWithQuote, coalWithQuote).forEach { brief ->
            pairs.forEach { pair ->
                assertEquals(emptyList(), TextProblems.of(pair.roast, RoastTextKind.ROAST, brief, TestRoasts.catalog.rules).toList(), pair.roast)
                assertEquals(emptyList(), TextProblems.of(pair.flip, RoastTextKind.FLIP, brief, TestRoasts.catalog.rules).toList(), pair.flip)
            }
        }
        fallback.story.values.flatten().forEach { line ->
            assertEquals(emptyList(), TextProblems.of(line, RoastTextKind.STORY, gift, TestRoasts.catalog.rules).toList(), line)
        }
    }

    @Test
    fun `대체 짝 key는 사실 규칙의 지표 방향을 모두 덮는다`() {
        val keys = TestRoasts.catalog.facts.toList().flatMap { spec -> listOf(spec.key, spec.key + "_low") }
        val missing = keys.filterNot { key -> key in TestRoasts.catalog.fallback.pairs } - setOf("reply_median_minutes")

        assertFalse(missing.isNotEmpty(), missing.toString())
    }

    private fun review(brief: RoastBrief, json: String): RoastReview = RoastReview.of(json, brief, TestRoasts.catalog.rules, TestRoasts.mapper)

    /** [keys]는 리뷰할 입력의 사실 key다. 사실은 집계로 섞은 시드로 고르므로 말풍선을 더한 입력은 순서가 다를 수 있다. */
    private fun validJson(
        keys: List<String> = factKeys,
        title: String = "단톡방 공식 물음표 자판기",
        verdict: String = "ㄱ? 치고 판을 깔던 손을 봤다. 선물 명단에 이름 적어 둔다.",
        verdictMild: String = "올해 말풍선 잘 읽었다. 선물은 문 앞에 잘 챙겨 두고 간다.",
        letter: String = "올해 네 말풍선을 끝까지 읽었다. 할 말이 생기면 미루지 않고 먼저 꺼내던 버릇이 좋았다. 내년에도 그대로 둬라.",
        firstFlip: String = "근데 망설이지 않고 먼저 판을 열었다. 그 손이 제일 빨랐다.",
        secondKey: String = keys[1],
        thirdKey: String = keys[2],
    ): String = TestRoasts.mapper.writeValueAsString(
        mapOf(
            "title" to title, "verdict" to verdict, "verdict_mild" to verdictMild, "letter" to letter,
            "pairs" to listOf(
                pair(keys[0], "'ㄱ?' 없이는 말이 안 시작된다. 거의 출발 신호다.", firstFlip),
                pair(secondKey, "웃음을 치기 시작하면 손을 뗄 줄 모른다. 키보드가 고생이다.", "근데 웃길 때는 끝까지 크게 웃었다. 그게 다 남았다."),
                pair(thirdKey, "물음표가 말끝에 자꾸 매달린다. 취조실이 따로 없다.", "근데 궁금한 건 그냥 넘기지 않고 꼭 물어봤다."),
                pair(keys[3], "대답을 짧게 끊어 던진다. 말풍선이 거의 신호등이다.", "근데 망설이지 않고 바로바로 답을 던졌다. 빠른 손이었다."),
                pair(keys[4], "말풍선을 쪼개서 연달아 보낸다. 알림이 줄줄이 울린다.", "근데 생각나는 대로 바로바로 꺼내 놓았다. 숨김이 없었다."),
            ),
            "story" to listOf(story(1, "새해 들어서는 판부터 깔자고 졸랐다."), story(2, "한여름에는 조용히 숨을 골랐다."), story(3, "연말에는 다시 먼저 말을 꺼냈다.")),
        ),
    )

    private fun pair(key: String, roast: String, flip: String) = mapOf("fact_key" to key, "roast" to roast, "flip" to flip)

    private fun story(act: Int, line: String) = mapOf("act" to act, "line" to line)

    companion object {
        private const val SNOWFLAKE = "\u2744"

        /** 인용 후보 최대 길이(quotable_max_length)의 입버릇. 사전에 있는 말이라 자주 쓰면 인용 후보가 된다. */
        private const val FOUR_CHAR_QUOTE = "아 근데"
        private const val HABIT_LINES = 60
    }

    private fun messages() = TestDossiers.selves(
        List(150) { index -> listOf("내일 ㄱ?", "ㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋ", "언제 볼까?", "ㄱㄱ 바로 출발", "넹")[index % 5] },
        minutesApart = 3,
    )
}
