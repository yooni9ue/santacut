package com.santacut.domain.prompt.model

import com.santacut.domain.roast.model.RoastTextKind
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 팩폭 작가 프롬프트 시드. 1판은 사람 이름만 가리는 입력 규칙과 연말 장부·다양성 지시를 담는다.
 * 2판은 1판을 그대로 두고 첫 응답이 떨어지던 까닭(글자 수, 수 세는 말, 목록 밖 인용)과 문장부호·사건 단정·오타 지시를 더한다.
 * 3판은 2판에 v3 말투 규칙(짧은 반말, "근데 "로 여는 뒤집기, 등급 온도)을 더하고, 서버 검증과 같은 글자 수 범위를 준다.
 */
class RoastPromptSeedTest {
    private val sql: String by lazy { Files.readString(Path.of("src/main/resources/prompts.sql")) }
    private val prompt: String by lazy { contentOf(HEAD) }
    private val second: String by lazy { contentOf(SECOND_HEAD) }
    private val third: String by lazy { contentOf(THIRD_HEAD) }
    private val fourth: String by lazy { contentOf(FOURTH_HEAD) }

    private fun contentOf(head: String): String {
        val start = sql.indexOf(head).also { index -> check(index >= 0) { "시드 없음: $head" } } + head.length
        return sql.substring(start, sql.indexOf("');", start)).replace("''", "'")
    }

    @Test
    fun `입력 표기는 지금 가리는 것만 적고 지명·돈·직장 이야기를 막지 않는다`() {
        assertTrue(prompt.contains("(이름), (집), (숫자), (링크), (영문)"))
        assertFalse(prompt.contains("(장소)"))
        assertFalse(prompt.contains("직장과 휴가"))
        assertTrue(prompt.contains("지명, 가게, 행사 이름은 말풍선에 나온 대로 써도 된다"))
        assertFalse(prompt.contains("\"다들\""))
    }

    @Test
    fun `연말 장부 맛과 짝·줄거리 다양성, 글자 수 세기 금지를 지시한다`() {
        assertTrue(prompt.contains("# 연말 장부의 맛"))
        assertTrue(prompt.contains("두 개까지만"))
        assertTrue(prompt.contains("\"판을 깔다\""))
        assertTrue(prompt.contains("\"두 글자\""))
        assertTrue(prompt.contains("세 막은 서로 다른 일을 다룬다"))
    }

    @Test
    fun `2판은 1판의 입력·안전 규칙을 그대로 지닌다`() {
        assertTrue(second.contains("(이름), (집), (숫자), (링크), (영문)"))
        assertTrue(second.contains("# 연말 장부의 맛"))
        assertTrue(second.contains("실제 사람 이름과 별명을 쓰지 않는다"))
        assertTrue(second.contains("\"상대\", \"모두\", \"남들\", \"친구\", \"우리\""))
        assertTrue(second.contains("욕설과 비속어는 인용하지도 쓰지도 않는다"))
    }

    @Test
    fun `2판은 필드별 글자 수 범위와 목표 길이, 길이 자를 준다`() {
        assertTrue(second.contains("# 글자 수"))
        assertTrue(second.contains("18자 이상 40자 이하, 목표는 24~34자다"))
        assertTrue(second.contains("8자 이상 18자 이하, 목표는 10~15자다"))
        assertTrue(second.contains("60자 이상 120자 이하, 목표는 80~105자다"))
        assertTrue(second.contains("16자 이상 34자 이하, 목표는 20~28자다"))
        val ruler = Regex("""- (\d+)자: 「([^」]+)」""").findAll(second).toList()
        assertEquals(listOf(20, 30, 40), ruler.map { match -> match.groupValues[1].toInt() })
        ruler.forEach { match -> assertEquals(match.groupValues[1].toInt(), match.groupValues[2].length) }
    }

    @Test
    fun `2판은 수 세는 말, 목록 밖 인용, 문장부호, 사건 단정, 오타를 따로 지시한다`() {
        assertTrue(second.contains("\"한 번\", \"두 번\", \"한두 번\""))
        assertTrue(second.contains("\"한 번 던지고\"는 \"툭 던지고\""))
        assertTrue(second.contains("말풍선에 글자 그대로 있으면 따옴표로 써도 된다"))
        assertTrue(second.contains("# 문장부호"))
        assertTrue(second.contains("놀림과 뒤집기 열 문장은 특히 끝을 똑같이 맞춘다"))
        assertTrue(second.contains("\"실패했다\""))
        assertTrue(second.contains("오타 없이 쓴다"))
        assertFalse(second.contains("두세 문장"))
    }

    @Test
    fun `3판은 말투 규칙과 2판의 입력·안전 규칙을 함께 지닌다`() {
        assertTrue(third.contains("# 말투 규칙"))
        assertTrue(third.contains("'~하거라', '~보아라', '~하였다', '~구나', '~습니다'"))
        assertTrue(third.contains("letter의 마지막 문장에서만 '~마', '~거라'를 써도 된다"))
        assertTrue(third.contains("(이름), (집), (숫자), (링크), (영문)"))
        assertTrue(third.contains("실제 사람 이름과 별명을 쓰지 않는다"))
        assertTrue(third.contains("욕설과 비속어는 인용하지도 쓰지도 않는다"))
    }

    @Test
    fun `3판은 '말끝마다'를 level이 훨씬일 때만 허용해 말투 규칙과 반드시 지킬 것이 어긋나지 않는다`() {
        assertTrue(third.contains("'말끝마다', '밤만 되면'처럼 늘 그렇다는 말은 그 사실의 level이 '훨씬'일 때만 쓰고"))
        assertTrue(third.contains("\"말끝마다\", \"매번\", \"항상\", \"모든\"은 그 사실의 level이 \"훨씬\"일 때만 쓴다"))
        assertFalse(third.contains("양은 '말끝마다', '밤만 되면', '듣기도 전에'로 말한다"))
    }

    @Test
    fun `3판의 뒤집기는 근데로 시작하고 글자 수 범위가 서버 검증과 같다`() {
        assertTrue(third.contains("반드시 \"근데 \"로 시작한다"))
        assertTrue(third.contains("화면은 \"근데\"를 따로 붙이지 않으니"))
        listOf(RoastTextKind.ROAST, RoastTextKind.FLIP, RoastTextKind.TITLE, RoastTextKind.VERDICT, RoastTextKind.LETTER, RoastTextKind.STORY)
            .forEach { kind -> assertTrue(third.contains("${kind.minLength}자 이상 ${kind.maxLength}자 이하"), kind.name) }
        val ruler = Regex("""- (\d+)자: 「([^」]+)」""").findAll(third).toList()
        ruler.forEach { match -> assertEquals(match.groupValues[1].toInt(), match.groupValues[2].length) }
    }

    @Test
    fun `3판은 작가 입력의 등급 온도를 따르고 셈말과 붙여 쓴 한마디를 구분한다`() {
        assertTrue(third.contains("- temperature: 등급 온도다."))
        assertTrue(third.contains("\"아슬아슬 통과\"(SCRAPED_THROUGH)"))
        assertTrue(third.contains("'아슬아슬 통과'에 '넉넉하다'고 쓰지 않는다"))
        assertTrue(third.contains("\"한 포대\", \"세 줄\", \"하나\", \"첫째\""))
        assertTrue(third.contains("띄어 쓴 \"한 마디\"는 수를 세는 말이다"))
    }

    @Test
    fun `4판은 3판의 말투·안전 규칙을 지니고 유행어는 입력 slang 목록에서 필드마다 하나까지만 쓰게 한다`() {
        assertTrue(fourth.contains("'~하거라', '~보아라', '~하였다', '~구나', '~습니다'"))
        assertTrue(fourth.contains("실제 사람 이름과 별명을 쓰지 않는다"))
        assertTrue(fourth.contains("반드시 \"근데 \"로 시작한다"))
        assertTrue(fourth.contains("- slang: 써도 되는 유행어 표기와 그 뜻."))
        assertTrue(fourth.contains("필드 하나에 하나까지만 쓰고, 모든 필드에 넣지 않는다"))
        assertTrue(fourth.contains("유행어를 빼고 읽어도 웃겨야 한다"))
        assertTrue(fourth.contains("성별, 나이, 세대, 사는 곳, 외모, 형편(돈), 마음 건강"))
        assertFalse(fourth.contains("유행어('갑'"))
    }

    @Test
    fun `4판은 밈 틀과 앞의 세 짝 계열 규칙을 주고 글자 수 범위는 서버 검증과 같다`() {
        listOf("- 대조형:", "- 헤드라인형:", "- 번복형:", "- 칭호형:", "- 선언형:").forEach { frame -> assertTrue(fourth.contains(frame), frame) }
        assertTrue(fourth.contains("앞의 세 짝은 group이 모두 다른 사실로 쓰고"))
        listOf(RoastTextKind.ROAST, RoastTextKind.FLIP, RoastTextKind.TITLE, RoastTextKind.VERDICT, RoastTextKind.LETTER, RoastTextKind.STORY)
            .forEach { kind -> assertTrue(fourth.contains("${kind.minLength}자 이상 ${kind.maxLength}자 이하"), kind.name) }
    }

    companion object {
        private const val FOURTH_HEAD = "VALUES ('7f3c2a10-0d4e-4c55-9b61-5c0f2e8b0004','roast',4,'"
        private const val HEAD = "VALUES ('7f3c2a10-0d4e-4c55-9b61-5c0f2e8b0001','roast',1,'"
        private const val SECOND_HEAD = "VALUES ('7f3c2a10-0d4e-4c55-9b61-5c0f2e8b0002','roast',2,'"
        private const val THIRD_HEAD = "VALUES ('7f3c2a10-0d4e-4c55-9b61-5c0f2e8b0003','roast',3,'"
    }
}
