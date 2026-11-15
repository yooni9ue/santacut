package com.santacut.domain.ranking.model

/**
 * 순위표 한 페이지.
 */
class RankingRows(private val values: List<RankingRow>) {
    /** 순위표 순서대로 각 줄을 바꾼 결과. */
    fun <T> map(transform: (RankingRow) -> T): List<T> = values.map(transform)

    companion object {
        fun empty(): RankingRows = RankingRows(emptyList())
    }
}
