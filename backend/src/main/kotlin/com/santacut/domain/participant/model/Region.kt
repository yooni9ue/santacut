package com.santacut.domain.participant.model

import com.santacut.domain.global.model.Coordinate
import com.santacut.domain.global.model.ForeignKeyColumns
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * 지역(시·도 또는 시·군·구)과 대표 좌표. schema.sql(서울)과 regions.sql(그 밖의 시·도)이 채우고 앱이 뜰 때 상위 지역만 잇는다.
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

    @Column(name = "parent_code", columnDefinition = ForeignKeyColumns.REGION)
    var parentCode: String? = parentCode
        protected set

    fun coordinate(): Coordinate = Coordinate(latitude, longitude)

    fun isChildOf(parentCode: String?): Boolean = this.parentCode == parentCode

    /** 코드 체계상 [other]의 상위 지역인지. 상위 지역 코드에 구분자를 붙인 것이 하위 코드의 접두어다(`KR-11` → `KR-11-110`). */
    fun isParentOf(other: Region): Boolean = other.code.startsWith(code + CODE_SEPARATOR)

    /** 상위 지역이 이미 이어졌는지. */
    fun isLinked(): Boolean = parentCode != null

    /** [parent]를 상위 지역으로 잇고 자신을 돌려준다. */
    fun linkTo(parent: Region): Region {
        parentCode = parent.code
        return this
    }

    companion object {
        private const val CODE_SEPARATOR = "-"
    }
}
