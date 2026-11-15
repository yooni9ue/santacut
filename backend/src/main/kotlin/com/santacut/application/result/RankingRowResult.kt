package com.santacut.application.result

import com.santacut.domain.model.RankingRow
import java.time.Instant

/**
 * 순위표 한 줄.
 */
data class RankingRowResult(
    val nickname: String,
    val rank: Int,
    val scoreHundredths: Int,
    val eta: Instant,
    val computedAt: Instant,
) {
    companion object {
        fun from(row: RankingRow): RankingRowResult =
            RankingRowResult(row.nickname, row.rank, row.scoreHundredths, row.eta, row.computedAt)
    }
}
