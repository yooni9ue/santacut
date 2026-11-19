package com.santacut.domain.dossier.model

import com.santacut.domain.analysis.model.ScoringMessage
import java.time.LocalDate

/**
 * 본인 메시지의 시각 분포를 쌓는다(한국 시각). 시간대는 보낸 시각, 달·요일은 [DossierCalendar]의 날짜로 센다.
 */
class ClockCounter {
    private val hourly = Counts.ofSize(HOURS)
    private val monthly = Counts.ofSize(MONTHS)
    private val weekday = Counts.ofSize(WEEKDAYS)

    fun add(message: ScoringMessage, day: LocalDate) {
        hourly.add(message.localTime.hour)
        monthly.add(day.monthValue - 1)
        weekday.add(day.dayOfWeek.ordinal)
    }

    /** 쌓은 분포. 심야 비율의 분모는 본인 메시지 수 [messageCount]다. */
    fun stats(messageCount: Int): ClockStats {
        val busiest = monthly.peakIndex()
        val quietest = monthly.quietestIndex()
        return ClockStats(
            hourly.toList(), monthly.toList(), weekday.toList(), hourly.peakIndex(),
            DossierRatios.ratio(hourly.totalIn(NIGHT_HOURS), messageCount), busiest?.plus(1), quietest?.plus(1),
            monthRatio(busiest, quietest),
        )
    }

    private fun monthRatio(busiest: Int?, quietest: Int?): Double? {
        if (busiest == null || quietest == null) return null
        return DossierRatios.tenth(monthly[busiest].toDouble() / monthly[quietest])
    }

    companion object {
        private const val HOURS = 24
        private const val MONTHS = 12
        private const val WEEKDAYS = 7

        /** 0시부터 5시 전까지. */
        private val NIGHT_HOURS = 0..4
    }
}
