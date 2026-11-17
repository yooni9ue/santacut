package com.santacut.domain.ranking.model

/**
 * 순위 스냅숏 목록.
 */
class ResultSnapshots(private val values: List<ResultSnapshot>) {
    fun isEmpty(): Boolean = values.isEmpty()

    /** 저장에 넘길 방어 복사본. */
    fun toList(): List<ResultSnapshot> = values.toList()
}
