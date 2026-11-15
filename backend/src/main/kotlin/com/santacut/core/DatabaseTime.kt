package com.santacut.core

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val databaseTimestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    .withZone(ZoneOffset.UTC)

fun utcDatabaseTime(instant: Instant): String = databaseTimestamp.format(instant)

fun parseUtcDatabaseTime(value: String): Instant = LocalDateTime.parse(value, databaseTimestamp).toInstant(ZoneOffset.UTC)
