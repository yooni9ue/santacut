package com.santacut.infrastructure.participant.repository

import com.santacut.domain.participant.model.RegionChange
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface RegionChangeRepository : JpaRepository<RegionChange, String> {
    fun countByUserId(userId: String): Long

    fun countByUserIdAndChangedAtAfter(userId: String, changedAt: Instant): Long

    fun findFirstByUserIdAndChangedAtAfterOrderByChangedAtAsc(userId: String, changedAt: Instant): RegionChange?
}
