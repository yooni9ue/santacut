package com.santacut.domain.model

/**
 * 높은 구간부터 정렬한 점수 판정 목록.
 */
class ScoreVerdicts(private val values: List<ScoreVerdict>) {
    /** 점수가 처음으로 하한을 넘는 판정. 어느 구간에도 들지 않으면 null이다. */
    fun find(hundredths: Int): ScoreVerdict? = values.firstOrNull { it.covers(hundredths) }
}
