package com.santacut.domain.analysis.model

import java.time.LocalDate

/**
 * 주별 라운드 로빈에서 메시지 하나의 자리. 모든 주의 [round]번째 메시지를 주 순서대로 뽑은 뒤 다음 라운드로 넘어간다.
 *
 * @property round 그 주 안에서 뽑히는 순번(0부터)
 * @property week 메시지가 든 주의 월요일
 */
data class SampleSlot(
    val round: Int,
    val week: LocalDate,
    val message: ScoringMessage,
) {
    companion object {
        val ORDER: Comparator<SampleSlot> = compareBy<SampleSlot> { it.round }.thenBy { it.week }
    }
}
