package com.santacut.domain.global.model

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * 데이터베이스에 저장하는 UTC 시각 문자열(`yyyy-MM-dd HH:mm:ss.SSS`) 형식.
 * 문자열 비교 순서가 시간 순서와 같아서 쿼리에서 그대로 비교할 수 있다.
 */
object UtcDatabaseTime {
    /** 값을 주지 않고 넣으면(시드 SQL 등) DB가 지금 시각을 같은 형식으로 채우는 생성 시각 컬럼 정의. */
    const val CREATED_AT_COLUMN_DEFINITION = "TEXT NOT NULL DEFAULT (strftime('%Y-%m-%d %H:%M:%f','now'))"

    private val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        .withZone(ZoneOffset.UTC)

    fun format(instant: Instant): String = FORMATTER.format(instant)

    fun parse(value: String): Instant = LocalDateTime.parse(value, FORMATTER).toInstant(ZoneOffset.UTC)
}
