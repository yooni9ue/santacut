package com.santacut.domain.ranking.model

import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RankingRowsTest {
    private val eta = Instant.parse("2026-12-25T00:03:00Z")
    private val computedAt = Instant.parse("2026-12-01T00:00:00Z")

    @Test
    fun `순위표 줄은 받은 순서대로 바꾼다`() {
        val rows = RankingRows(listOf(row("첫째", 1), row("둘째", 2), row("셋째", 2)))
        assertEquals(listOf("첫째", "둘째", "셋째"), rows.map { it.nickname })
        assertEquals(listOf(1, 2, 2), rows.map { it.rank })
    }

    @Test
    fun `빈 순위표는 바꿔도 아무 줄이 없다`() {
        assertTrue(RankingRows.empty().map { it.nickname }.isEmpty())
    }

    private fun row(nickname: String, rank: Int): RankingRow = RankingRow(nickname, rank, 8000, eta, computedAt)
}
