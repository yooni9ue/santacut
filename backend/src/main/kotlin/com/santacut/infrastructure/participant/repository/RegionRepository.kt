package com.santacut.infrastructure.participant.repository

import com.santacut.domain.participant.model.Region
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface RegionRepository : JpaRepository<Region, String> {
    fun findAllByOrderByCodeAsc(): List<Region>

    /** 참가자의 지금 지역 이름. 참가자가 없으면 null이다. */
    @Query("select r.name from Participant p, Region r where p.id = :participantId and r.code = p.regionCode")
    fun findNameOfParticipantRegion(participantId: String): String?
}
