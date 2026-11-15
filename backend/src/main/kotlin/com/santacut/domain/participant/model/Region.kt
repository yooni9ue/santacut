package com.santacut.domain.participant.model

import com.santacut.domain.global.model.Coordinate
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * 참가 가능한 지역(서울 자치구)과 대표 좌표. schema.sql이 채우는 읽기 전용 데이터다.
 */
@Entity
@Table(name = "regions")
class Region(
    code: String,
    name: String,
    latitude: Double,
    longitude: Double,
) {
    @Id
    @Column(name = "code")
    var code: String = code
        protected set

    @Column(name = "name", nullable = false)
    var name: String = name
        protected set

    @Column(name = "latitude", nullable = false)
    var latitude: Double = latitude
        protected set

    @Column(name = "longitude", nullable = false)
    var longitude: Double = longitude
        protected set

    fun coordinate(): Coordinate = Coordinate(latitude, longitude)
}
