package com.santacut.application.ranking.result

import com.santacut.domain.ranking.model.RankingBoard
import java.time.Instant

/**
 * 순위표 한 페이지와 그 순위 계산의 메타 정보.
 *
 * @property stage 순위 확정 상태 이름(PREVIEW, FINAL)
 * @property nextCursor 다음 페이지 cursor. 마지막 페이지면 null이다.
 */
data class RankingBoardResult(
    val seasonYear: Int,
    val participantCount: Long,
    val computedAt: Instant?,
    val stage: String,
    val rows: RankingRowResults,
    val nextCursor: String?,
) {
    companion object {
        fun from(board: RankingBoard): RankingBoardResult = RankingBoardResult(
            board.seasonYear, board.participantCount, board.computedAt, board.stage.name,
            RankingRowResults.from(board.rows), board.nextCursor?.value(),
        )
    }
}
