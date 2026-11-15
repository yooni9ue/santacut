package com.santacut.domain.model

import com.santacut.domain.exception.ErrorCode
import com.santacut.domain.exception.SantaCutException
import java.time.Instant
import java.time.ZonedDateTime

/**
 * 한 해의 산타컷 시즌. 서울 시각 12월 25일 0시에 마감하고 배송을 시작한다.
 */
data class Season(val year: Int) {
    fun deliveryStart(): ZonedDateTime =
        ZonedDateTime.of(year, DECEMBER, CHRISTMAS_DAY, 0, 0, 0, 0, SeoulTime.ZONE)

    fun isClosedAt(instant: Instant): Boolean = !instant.isBefore(deliveryStart().toInstant())

    fun verifyOpenAt(instant: Instant) {
        if (isClosedAt(instant)) throw SantaCutException(ErrorCode.SEASON_FROZEN)
    }

    companion object {
        private const val DECEMBER = 12
        private const val CHRISTMAS_DAY = 25
    }
}
