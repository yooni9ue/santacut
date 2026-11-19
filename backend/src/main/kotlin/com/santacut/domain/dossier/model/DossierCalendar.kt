package com.santacut.domain.dossier.model

import com.santacut.domain.analysis.model.ScoringMessage
import java.time.LocalDate

/**
 * 조서가 쓰는 날짜 규칙. 하루 경계는 05:00이라 새벽 4시 메시지는 전날로 센다. 다만 분석 기간이 1월 1일 0시에 시작하므로
 * 1월 1일 0~5시 메시지는 지난해 12월 31일이 아니라 1월 1일로 센다(달·막·요일·활동일이 지난해로 새지 않게).
 */
object DossierCalendar {
    /** 이 시각 전 메시지는 전날로 센다. */
    const val DAY_BOUNDARY_HOURS = 5L

    fun dayOf(message: ScoringMessage): LocalDate {
        val shifted = message.localTime.minusHours(DAY_BOUNDARY_HOURS).toLocalDate()
        return if (shifted.year < message.localTime.year) message.localTime.toLocalDate() else shifted
    }
}
