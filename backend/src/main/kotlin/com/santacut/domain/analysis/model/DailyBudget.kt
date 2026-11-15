package com.santacut.domain.analysis.model

import com.santacut.domain.global.model.SeoulTime
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate

/**
 * 서울 날짜 하루 동안 쓴 AI 호출 수.
 */
@Entity
@Table(name = "ai_daily_budget")
class DailyBudget(
    day: String,
    calls: Int,
) {
    @Id
    @Column(name = "day")
    var day: String = day
        protected set

    @Column(name = "calls", nullable = false)
    var calls: Int = calls
        protected set

    fun hasRemaining(limit: Int): Boolean = calls < limit

    fun consume() {
        calls++
    }

    companion object {
        /** 시각이 속한 서울 날짜(`yyyy-MM-dd`). */
        fun dayOf(instant: Instant): String = LocalDate.ofInstant(instant, SeoulTime.ZONE).toString()

        fun startOf(day: String): DailyBudget = DailyBudget(day, 0)
    }
}
