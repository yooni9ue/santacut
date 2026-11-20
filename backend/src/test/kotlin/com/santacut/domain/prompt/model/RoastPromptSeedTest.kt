package com.santacut.domain.prompt.model

import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** 팩폭 작가 프롬프트 1판 시드. 사람 이름만 가리는 입력 규칙과 연말 장부·다양성 지시를 담는다. */
class RoastPromptSeedTest {
    private val prompt: String by lazy {
        val sql = Files.readString(Path.of("src/main/resources/prompts.sql"))
        val start = sql.indexOf(HEAD).also { index -> check(index >= 0) { "roast 1판 시드 없음" } } + HEAD.length
        sql.substring(start, sql.indexOf("');", start)).replace("''", "'")
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

    companion object {
        private const val HEAD = "VALUES ('7f3c2a10-0d4e-4c55-9b61-5c0f2e8b0001','roast',1,'"
    }
}
