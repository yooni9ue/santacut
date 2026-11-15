package com.santacut.domain.ranking.model

/**
 * 배송 순서로 읽은 순위표 줄. 한 페이지보다 한 줄 더 읽어 다음 페이지가 있는지 판단한다.
 */
class RankingRows(private val values: List<RankingRow>) {
    /** 순위표 순서대로 각 줄을 바꾼 결과. */
    fun <T> map(transform: (RankingRow) -> T): List<T> = values.map(transform)

    /** 앞에서부터 [size]줄만 남긴다. */
    fun limitTo(size: Int): RankingRows = RankingRows(values.take(size))

    /** [size]줄 뒤에 줄이 더 있으면 [size]번째 줄 다음부터 읽는 cursor, 없으면 null. */
    fun nextCursorAfter(size: Int): RankingCursor? =
        if (values.size > size) RankingCursor.after(values[size - 1].deliverySequence) else null

    companion object {
        fun empty(): RankingRows = RankingRows(emptyList())
    }
}
