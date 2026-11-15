package com.santacut.infrastructure.participant.repository

import com.santacut.domain.participant.model.Region
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface RegionRepository : JpaRepository<Region, String> {
    fun findAllByOrderByCodeAsc(): List<Region>

    /**
     * 상위 지역이 비어 있는 지역을 코드 접두어가 같은 가장 가까운 지역에 잇는다(`KR-11-110` → `KR-11`).
     * 이미 이어진 행은 건드리지 않으므로 여러 번 실행해도 결과가 같다. 접두어가 맞는 지역이 없으면 최상위로 남는다.
     */
    @Modifying
    @Query(
        nativeQuery = true,
        value = "UPDATE regions SET parent_code = (" +
            "SELECT parent.code FROM regions parent WHERE regions.code LIKE parent.code || '-%' " +
            "ORDER BY length(parent.code) DESC LIMIT 1) " +
            "WHERE parent_code IS NULL",
    )
    fun linkParentsByCodePrefix(): Int
}
