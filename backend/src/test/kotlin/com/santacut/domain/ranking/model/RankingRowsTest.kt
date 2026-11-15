package com.santacut.domain.ranking.model

import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RankingRowsTest {
    private val eta = Instant.parse("2026-12-25T00:03:00Z")

    @Test
    fun `순위표 줄은 받은 순서대로 바꾼다`() {
        val rows = RankingRows(listOf(row("첫째", 1, 1), row("둘째", 2, 2), row("셋째", 2, 3)))
        assertEquals(listOf("첫째", "둘째", "셋째"), rows.map { it.nickname })
        assertEquals(listOf(1, 2, 2), rows.map { it.rank })
    }

    @Test
    fun `빈 순위표는 바꿔도 아무 줄이 없다`() {
        assertTrue(RankingRows.empty().map { it.nickname }.isEmpty())
    }

    @Test
    fun `한 페이지보다 더 읽었으면 페이지 마지막 줄의 배송 순서가 다음 cursor다`() {
        val rows = RankingRows(listOf(row("a", 1, 4), row("b", 2, 5), row("c", 3, 6)))
        assertEquals(listOf("a", "b"), rows.limitTo(2).map { it.nickname })
        assertEquals("5", rows.nextCursorAfter(2)?.value())
        assertNull(rows.nextCursorAfter(3))
    }

    private fun row(nickname: String, rank: Int, sequence: Int): RankingRow =
        RankingRow(nickname, rank, false, 8000, eta, sequence)
}
