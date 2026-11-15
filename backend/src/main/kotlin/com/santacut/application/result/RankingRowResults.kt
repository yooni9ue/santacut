package com.santacut.application.result

import com.santacut.domain.model.RankingRows

/**
 * 순위표 한 페이지. 순위표 순서를 유지한다.
 */
class RankingRowResults(private val values: List<RankingRowResult>) {
    /** 순위표 순서대로 각 줄을 바꾼 결과. */
    fun <T> map(transform: (RankingRowResult) -> T): List<T> = values.map(transform)

    companion object {
        fun from(rows: RankingRows): RankingRowResults = RankingRowResults(rows.map(RankingRowResult::from))
    }
}
