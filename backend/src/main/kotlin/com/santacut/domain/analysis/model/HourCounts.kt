package com.santacut.domain.analysis.model

/**
 * 시간 구간별 메시지 수.
 */
class HourCounts(private val values: Map<HourBucket, Int>) {
    fun countOf(bucket: HourBucket): Int = values[bucket] ?: 0
}
