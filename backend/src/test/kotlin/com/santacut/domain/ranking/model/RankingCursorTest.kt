package com.santacut.domain.ranking.model

import com.santacut.ErrorCodeAssertions.assertErrorCode
import com.santacut.domain.global.exception.ErrorCode
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RankingCursorTest {
    @Test
    fun `cursor가 없으면 첫 페이지부터 50줄을 읽고 한 줄 더 가져온다`() {
        val cursor = RankingCursor.from(null)
        assertEquals(0, cursor.afterSequence)
        assertEquals(50, cursor.pageSize)
        assertEquals(51, cursor.fetchSize)
    }

    @Test
    fun `이전 응답의 cursor 문자열을 다시 읽으면 같은 위치다`() {
        val cursor = RankingCursor.after(50)
        assertEquals(50, RankingCursor.from(cursor.value()).afterSequence)
    }

    @Test
    fun `양의 정수가 아닌 cursor는 거절한다`() {
        listOf("", "abc", "0", "-3", "1.5").forEach { raw ->
            assertErrorCode(ErrorCode.INVALID_CURSOR) { RankingCursor.from(raw) }
        }
    }
}
