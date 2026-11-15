package com.santacut.infrastructure.repository

import com.santacut.domain.model.Region
import org.springframework.data.jpa.repository.JpaRepository

interface RegionRepository : JpaRepository<Region, String> {
    fun findAllByOrderByCodeAsc(): List<Region>
}
