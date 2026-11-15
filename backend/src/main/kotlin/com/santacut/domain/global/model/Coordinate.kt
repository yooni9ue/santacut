package com.santacut.domain.global.model

/**
 * 위도와 경도. 지구 위의 유효한 좌표만 만들 수 있다.
 */
data class Coordinate(val latitude: Double, val longitude: Double) {
    init {
        require(latitude.isFinite() && longitude.isFinite()) { "좌표는 유한한 숫자여야 합니다." }
        require(latitude in LATITUDE_RANGE && longitude in LONGITUDE_RANGE) { "좌표가 지구 범위를 벗어났습니다." }
    }

    companion object {
        private val LATITUDE_RANGE = -90.0..90.0
        private val LONGITUDE_RANGE = -180.0..180.0
    }
}
