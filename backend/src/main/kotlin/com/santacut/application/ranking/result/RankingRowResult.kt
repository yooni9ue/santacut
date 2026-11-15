package com.santacut.application.ranking.result

import com.santacut.domain.ranking.model.RankingRow
import java.time.Instant

/**
 * 순위표 한 줄.
 */
data class RankingRowResult(
    val nickname: String,
    val rank: Int,
    val tied: Boolean,
    val scoreHundredths: Int,
    val eta: Instant,
) {
    companion object {
        fun from(row: RankingRow): RankingRowResult =
            RankingRowResult(row.nickname, row.rank, row.tied, row.scoreHundredths, row.eta)
    }
}
