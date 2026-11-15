package com.santacut.domain.ranking.model

import com.santacut.domain.global.model.Coordinate
import java.time.ZonedDateTime
import kotlin.math.asin
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 산타 썰매 배송 시뮬레이션 규칙.
 * 시속 100km로 이동하고 정거장마다 3분을 쓰며, 하루에 6시간만 일한다.
 */
object Delivery {
    private const val EARTH_RADIUS_KM = 6371.0088
    private const val SLEIGH_SPEED_KMH = 100
    private const val SECONDS_PER_HOUR = 3600
    private const val STOP_SECONDS = 180
    private const val WORK_SECONDS_PER_DAY = 21600L

    /** 반각 공식에서 각을 반으로 나누는 값. */
    private const val HALF_ANGLE_DIVISOR = 2

    /** 중심각 = 2 × asin(√하버사인). 반지름을 곱하면 대원 거리이므로 지름 × asin(√하버사인)과 같다. */
    private const val DIAMETER_FACTOR = 2

    /** 반올림 오차로 하버사인 값이 벗어나지 않도록 묶는 유효 범위(asin의 정의역 안). */
    private val HAVERSINE_RANGE = 0.0..1.0

    /** 두 좌표 사이의 대원 거리(km). */
    fun distance(from: Coordinate, to: Coordinate): Double =
        EARTH_RADIUS_KM * centralAngle(haversine(from, to))

    /** 하버사인 공식으로 두 좌표 사이 중심각의 하버사인 값을 구한다. */
    private fun haversine(from: Coordinate, to: Coordinate): Double {
        val latitudeDelta = Math.toRadians(to.latitude - from.latitude)
        val longitudeDelta = Math.toRadians(to.longitude - from.longitude)
        return halfAngleSineSquared(latitudeDelta) +
            cos(Math.toRadians(from.latitude)) * cos(Math.toRadians(to.latitude)) * halfAngleSineSquared(longitudeDelta)
    }

    private fun halfAngleSineSquared(angle: Double): Double = sin(angle / HALF_ANGLE_DIVISOR).pow(2)

    /** 하버사인 값에서 중심각(라디안)을 되돌린다. */
    private fun centralAngle(haversine: Double): Double =
        DIAMETER_FACTOR * asin(sqrt(haversine.coerceIn(HAVERSINE_RANGE)))

    /** 한 정거장에서 다음 정거장까지 이동하고 배송하는 데 드는 초. */
    fun legSeconds(from: Coordinate, to: Coordinate): Double =
        distance(from, to) / SLEIGH_SPEED_KMH * SECONDS_PER_HOUR + STOP_SECONDS

    /** 누적 작업 시간을 하루 6시간씩 나눠 일했을 때의 도착 시각. */
    fun arrival(start: ZonedDateTime, workSeconds: Double): ZonedDateTime {
        require(workSeconds.isFinite() && workSeconds > 0 && workSeconds < Long.MAX_VALUE.toDouble()) {
            "작업 시간은 0보다 큰 유한한 초여야 합니다."
        }
        val roundedSeconds = ceil(workSeconds).toLong()
        val elapsedDays = (roundedSeconds - 1) / WORK_SECONDS_PER_DAY
        return start.plusDays(elapsedDays).plusSeconds(roundedSeconds - elapsedDays * WORK_SECONDS_PER_DAY)
    }
}
