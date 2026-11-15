package com.santacut.domain.model

import com.santacut.domain.exception.ErrorCode
import com.santacut.domain.exception.SantaCutException

/**
 * 순위표 조회 범위. 0부터 시작하는 offset에서 최대 [SIZE]줄을 읽는다.
 */
class RankingPage private constructor(val offset: Int) {
    val size: Int = SIZE

    companion object {
        private const val SIZE = 50
        private val OFFSET_RANGE = 0..10_000

        fun of(offset: Int): RankingPage {
            if (offset !in OFFSET_RANGE) throw SantaCutException(ErrorCode.INVALID_OFFSET)
            return RankingPage(offset)
        }
    }
}
