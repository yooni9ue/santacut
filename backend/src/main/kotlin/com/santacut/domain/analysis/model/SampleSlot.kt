package com.santacut.domain.analysis.model

import java.time.LocalDate

/**
 * 주별·칸별 라운드 로빈에서 메시지 하나의 자리. 모든 주의 모든 칸에서 [round]번째 메시지를 주, 칸 순서로 뽑은 뒤 다음 라운드로 넘어간다.
 *
 * @property round 그 주 그 칸 안에서 뽑히는 순번(0부터)
 * @property week 메시지가 든 주의 월요일
 * @property bucket 칸 순번. 항목 낱말 칸이 [ScoreTheme] 순서로 먼저, 낱말 없는 칸이 마지막이다.
 */
data class SampleSlot(
    val round: Int,
    val week: LocalDate,
    val bucket: Int,
    val message: ScoringMessage,
) {
    companion object {
        val ORDER: Comparator<SampleSlot> = compareBy<SampleSlot> { it.round }.thenBy { it.week }.thenBy { it.bucket }
    }
}
