package com.santacut.domain.prompt.model

import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 팩폭 작가 프롬프트 시드. 1판은 사람 이름만 가리는 입력 규칙과 연말 장부·다양성 지시를 담는다.
 * 2판은 1판을 그대로 두고 첫 응답이 떨어지던 까닭(글자 수, 수 세는 말, 목록 밖 인용)과 문장부호·사건 단정·오타 지시를 더한다.
 */
class RoastPromptSeedTest {
    private val sql: String by lazy { Files.readString(Path.of("src/main/resources/prompts.sql")) }
    private val prompt: String by lazy { contentOf(HEAD) }
    private val second: String by lazy { contentOf(SECOND_HEAD) }

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
        assertTrue(second.contains("목록에 그대로 있을 때만 따옴표로 쓰고"))
        assertTrue(second.contains("# 문장부호"))
        assertTrue(second.contains("놀림과 뒤집기 열 문장은 특히 끝을 똑같이 맞춘다"))
        assertTrue(second.contains("\"실패했다\""))
        assertTrue(second.contains("오타 없이 쓴다"))
        assertFalse(second.contains("두세 문장"))
    }

    companion object {
        private const val HEAD = "VALUES ('7f3c2a10-0d4e-4c55-9b61-5c0f2e8b0001','roast',1,'"
        private const val SECOND_HEAD = "VALUES ('7f3c2a10-0d4e-4c55-9b61-5c0f2e8b0002','roast',2,'"
    }
}
