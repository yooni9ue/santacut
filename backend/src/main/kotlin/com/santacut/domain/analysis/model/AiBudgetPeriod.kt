package com.santacut.domain.analysis.model

import com.santacut.domain.global.model.SeoulTime
import java.time.Instant
import java.time.LocalDate

/**
 * 비용 한도를 세는 기간. 한국 시각 날짜 하루가 하루 한도의 기간이고, 그 달 1일부터 그날까지가 한 달 한도의 기간이다.
 * 사용량은 날짜별 행([AiUsageDay])에 쌓으므로 기간은 날짜 키의 처음과 끝으로 나타낸다.
 *
 * @property day 한국 시각 기준 오늘
 */
data class AiBudgetPeriod(
    val day: LocalDate,
) {
    /** 오늘 사용량을 쌓는 행의 키. 하루 기간의 처음이자 끝이다. */
    fun dayKey(): String = AiUsageDay.keyOf(day)

    /** 이번 달 기간의 처음(1일) 키. 끝은 [dayKey]다. */
    fun monthStartKey(): String = AiUsageDay.keyOf(day.withDayOfMonth(FIRST_DAY_OF_MONTH))

    companion object {
        private const val FIRST_DAY_OF_MONTH = 1

        fun at(now: Instant): AiBudgetPeriod = AiBudgetPeriod(LocalDate.ofInstant(now, SeoulTime.ZONE))
    }
}
