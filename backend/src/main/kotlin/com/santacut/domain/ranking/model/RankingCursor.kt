package com.santacut.domain.ranking.model

import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException

/**
 * 순위표 페이지 위치. 직전 페이지 마지막 줄의 배송 순서 다음부터 최대 [PAGE_SIZE]줄을 읽는다.
 * 클라이언트에는 불투명한 문자열([value])로만 주고받는다.
 *
 * @property afterSequence 이 배송 순서보다 뒤의 줄만 읽는다. 첫 페이지는 0이다.
 */
class RankingCursor private constructor(val afterSequence: Int) {
    val pageSize: Int = PAGE_SIZE

    /** 다음 페이지가 있는지 알기 위해 한 줄 더 읽는다. */
    val fetchSize: Int = PAGE_SIZE + 1

    /** 응답의 next_cursor로 내보내는 문자열. */
    fun value(): String = afterSequence.toString()

    companion object {
        private const val PAGE_SIZE = 50
        private const val FIRST_PAGE_SEQUENCE = 0

        fun first(): RankingCursor = RankingCursor(FIRST_PAGE_SEQUENCE)

        fun after(deliverySequence: Int): RankingCursor = RankingCursor(deliverySequence)

        /** 요청의 cursor. 없으면 첫 페이지이고, 이전 응답이 준 값이 아니면 [ErrorCode.INVALID_CURSOR]다. */
        fun from(raw: String?): RankingCursor {
            if (raw == null) return first()
            val sequence = raw.toIntOrNull()
            if (sequence == null || sequence <= FIRST_PAGE_SEQUENCE) throw SantaCutException(ErrorCode.INVALID_CURSOR)
            return after(sequence)
        }
    }
}
