package com.santacut.domain.analysis.model

/**
 * 채점 항목 순서의 낱말 신호 목록.
 */
class DimensionSignals(private val values: List<DimensionSignal>) {
    fun toList(): List<DimensionSignal> = values.toList()

    /** 항목 순서대로 각 신호를 바꾼다. */
    fun <T> map(transform: (DimensionSignal) -> T): List<T> = values.map(transform)
}
