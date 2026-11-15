package com.santacut.domain.participant.model

import com.santacut.domain.global.model.Coordinate
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * 지역(시·도 또는 시·군·구)과 대표 좌표. schema.sql이 채우는 읽기 전용 데이터다.
 * 시·도는 [parentCode]가 없고, 시·군·구는 [parentCode]에 시·도 코드를 가진다.
 */
@Entity
@Table(name = "regions")
class Region(
    code: String,
    name: String,
    latitude: Double,
    longitude: Double,
    parentCode: String? = null,
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

    @Column(name = "parent_code")
    var parentCode: String? = parentCode
        protected set

    fun coordinate(): Coordinate = Coordinate(latitude, longitude)

    fun isChildOf(parentCode: String?): Boolean = this.parentCode == parentCode
}
