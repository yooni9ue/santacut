package com.santacut.application.ranking.result

import com.santacut.domain.ranking.model.RankingRow
import java.time.LocalDate

/**
 * 순위표 한 줄. 공개 화면이므로 도착 예정은 서울 날짜([etaDate])만 담는다.
 */
data class RankingRowResult(
    val nickname: String,
    val rank: Int,
    val tied: Boolean,
    val scoreHundredths: Int,
    val etaDate: LocalDate,
) {
    companion object {
        fun from(row: RankingRow): RankingRowResult =
            RankingRowResult(row.nickname, row.rank, row.tied, row.scoreHundredths, row.etaDate())
    }
}
