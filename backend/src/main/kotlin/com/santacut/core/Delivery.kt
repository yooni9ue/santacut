package com.santacut.core

import java.time.ZonedDateTime
import kotlin.math.*

data class Coordinate(val latitude: Double, val longitude: Double) {
    init { require(latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0) }
}
object Delivery {
    fun distance(a: Coordinate, b: Coordinate): Double {
        val lat = Math.toRadians(b.latitude - a.latitude)
        val lon = Math.toRadians(b.longitude - a.longitude)
        val h = sin(lat / 2).pow(2) + cos(Math.toRadians(a.latitude)) * cos(Math.toRadians(b.latitude)) * sin(lon / 2).pow(2)
        return 6371.0088 * 2 * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }
    fun arrival(start: ZonedDateTime, workSeconds: Double): ZonedDateTime {
        require(workSeconds.isFinite() && workSeconds > 0 && workSeconds < Long.MAX_VALUE.toDouble())
        val rounded = ceil(workSeconds).toLong()
        val days = (rounded - 1) / 21600
        return start.plusDays(days).plusSeconds(rounded - days * 21600)
    }
}
