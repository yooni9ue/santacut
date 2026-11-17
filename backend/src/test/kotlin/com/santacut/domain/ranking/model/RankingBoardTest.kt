package com.santacut.domain.ranking.model

import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RankingBoardTest {
    private val season = Season(SEASON_YEAR)
    private val beforeClose = Instant.parse("2026-12-01T00:00:00Z")
    private val afterClose = Instant.parse("2026-12-26T00:00:00Z")
    private val computedAt = Instant.parse("2026-11-30T00:00:00Z")

    @Test
    fun `한 줄 더 읽었으면 한 페이지만 남기고 마지막 줄의 배송 순서를 다음 cursor로 준다`() {
        val cursor = RankingCursor.first()
        val board = RankingBoard.of(season, beforeClose, 120, computedAt, rows(cursor.fetchSize), cursor)

        assertEquals(cursor.pageSize, board.rows.map { it.nickname }.size)
        assertEquals(cursor.pageSize, board.nextCursor?.afterSequence)
        assertEquals(cursor.pageSize, RankingCursor.from(board.nextCursor?.value()).afterSequence)
        assertEquals(SEASON_YEAR, board.seasonYear)
        assertEquals(120, board.participantCount)
        assertEquals(computedAt, board.computedAt)
        assertEquals(RankingStage.PREVIEW, board.stage)
    }

    @Test
    fun `한 페이지 안에 다 들어오면 모든 줄을 주고 다음 cursor가 없다`() {
        val board = RankingBoard.of(season, afterClose, 3, computedAt, rows(3), RankingCursor.first())

        assertEquals(listOf("참가자1", "참가자2", "참가자3"), board.rows.map { it.nickname })
        assertNull(board.nextCursor)
        assertEquals(RankingStage.FINAL, board.stage)
    }

    @Test
    fun `순위를 계산한 적이 없으면 참가자 0명, 계산 시각 없음, 빈 줄, 다음 cursor 없음이다`() {
        val board = RankingBoard.empty(season, beforeClose)

        assertEquals(SEASON_YEAR, board.seasonYear)
        assertEquals(0, board.participantCount)
        assertNull(board.computedAt)
        assertTrue(board.rows.map { it.nickname }.isEmpty())
        assertNull(board.nextCursor)
        assertEquals(RankingStage.PREVIEW, board.stage)
    }

    private fun rows(count: Int): RankingRows =
        RankingRows(List(count) { index -> RankingRow("참가자${index + 1}", index + 1, false, 8000, computedAt, index + 1) })

    companion object {
        private const val SEASON_YEAR = 2026
    }
}
