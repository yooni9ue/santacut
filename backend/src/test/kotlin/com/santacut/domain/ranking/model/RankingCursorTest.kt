package com.santacut.domain.ranking.model

import com.santacut.ErrorCodeAssertions.assertErrorCode
import com.santacut.domain.global.exception.ErrorCode
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class RankingCursorTest {
    @Test
    fun `cursor가 없으면 첫 페이지부터 50줄을 읽고 한 줄 더 가져온다`() {
        val cursor = RankingCursor.from(null)
        assertEquals(0, cursor.afterSequence)
        assertEquals(50, cursor.pageSize)
        assertEquals(51, cursor.fetchSize)
    }

    @Test
    fun `이전 응답의 cursor 문자열을 다시 읽으면 같은 순위 계산의 같은 위치다`() {
        val cursor = RankingCursor.from(RankingCursor.after(50, COMPUTED_AT).value())
        assertEquals(50, cursor.afterSequence)
        assertFalse(cursor.isFirstPage())
        cursor.verifySnapshot(COMPUTED_AT)
    }

    @Test
    fun `cursor를 만든 뒤 순위를 다시 계산했으면 거절한다`() {
        val cursor = RankingCursor.from(RankingCursor.after(50, COMPUTED_AT).value())
        assertErrorCode(ErrorCode.RANKING_SNAPSHOT_CHANGED) { cursor.verifySnapshot(COMPUTED_AT.plusMillis(1)) }
    }

    @Test
    fun `첫 페이지는 어느 순위 계산이든 읽는다`() {
        RankingCursor.first().verifySnapshot(COMPUTED_AT)
    }

    @Test
    fun `이전 응답이 준 값이 아니거나 예전 숫자 형식인 cursor는 거절한다`() {
        val invalid = listOf("", "abc", "0", "-3", "1.5", "51", encode("1.0"), encode("x.5"), encode("1.2.3"))
        invalid.forEach { raw -> assertErrorCode(ErrorCode.INVALID_CURSOR) { RankingCursor.from(raw) } }
    }

    private fun encode(raw: String): String = Base64.getUrlEncoder().withoutPadding().encodeToString(raw.toByteArray())

    companion object {
        private val COMPUTED_AT = Instant.parse("2026-12-01T00:00:00.123Z")
    }
}
