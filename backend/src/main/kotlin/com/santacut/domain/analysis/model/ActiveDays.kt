package com.santacut.domain.analysis.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 메시지를 하나라도 보낸 날짜 목록. 겹치지 않고 오름차순이다.
 */
class ActiveDays private constructor(private val values: List<LocalDate>) {
    fun size(): Int = values.size

    /** 하루도 빠지지 않고 이어진 가장 긴 날 수. 보낸 날이 없으면 0이다. */
    fun longestStreak(): Int {
        if (values.isEmpty()) return 0
        return values.zipWithNext { previous, next -> ChronoUnit.DAYS.between(previous, next) == ONE_DAY }
            .runningFold(1) { streak, consecutive -> if (consecutive) streak + 1 else 1 }
            .max()
    }

    companion object {
        private const val ONE_DAY = 1L

        fun of(dates: List<LocalDate>): ActiveDays = ActiveDays(dates.distinct().sorted())
    }
}
