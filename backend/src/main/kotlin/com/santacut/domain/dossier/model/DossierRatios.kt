package com.santacut.domain.dossier.model

import kotlin.math.roundToLong

/**
 * 조서 집계값의 반올림 규칙. 밀도와 평균은 소수 첫째 자리, 비율은 소수 셋째 자리(0~1)다.
 */
object DossierRatios {
    private const val PER_HUNDRED = 100.0
    private const val TENTHS = 10.0
    private const val THOUSANDTHS = 1000.0

    /** [whole]개당 [count]를 100개당으로 바꾼 값. [whole]이 0이면 0이다. */
    fun per100(count: Int, whole: Int): Double = if (whole == 0) 0.0 else tenth(count * PER_HUNDRED / whole)

    /** 0~1 사이 비율. [whole]이 0이면 0이다. */
    fun ratio(part: Int, whole: Int): Double = if (whole == 0) 0.0 else (part * THOUSANDTHS / whole).roundToLong() / THOUSANDTHS

    /** [total] ÷ [whole]의 평균. [whole]이 0이면 0이다. */
    fun average(total: Long, whole: Int): Double = if (whole == 0) 0.0 else tenth(total.toDouble() / whole)

    fun tenth(value: Double): Double = (value * TENTHS).roundToLong() / TENTHS
}
