package com.santacut.domain.ranking.model

/**
 * 순위 스냅숏 목록.
 */
class ResultSnapshots(private val values: List<ResultSnapshot>) {
    fun isEmpty(): Boolean = values.isEmpty()

    /** 주어진 ID의 스냅숏을 뺀 나머지. */
    fun excluding(snapshotIds: Set<String>): ResultSnapshots =
        ResultSnapshots(values.filterNot { it.id in snapshotIds })

    /** 저장·삭제에 넘길 방어 복사본. */
    fun toList(): List<ResultSnapshot> = values.toList()
}
