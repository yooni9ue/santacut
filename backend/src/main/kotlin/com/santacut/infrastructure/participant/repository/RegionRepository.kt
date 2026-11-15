package com.santacut.infrastructure.participant.repository

import com.santacut.domain.participant.model.Region
import org.springframework.data.jpa.repository.JpaRepository

interface RegionRepository : JpaRepository<Region, String> {
    fun findAllByOrderByCodeAsc(): List<Region>
}
