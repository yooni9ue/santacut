package com.santacut.infrastructure.participant.repository

import com.santacut.domain.participant.model.Participant
import org.springframework.data.jpa.repository.JpaRepository

interface ParticipantRepository : JpaRepository<Participant, String> {
    fun existsByNicknameKey(nicknameKey: String): Boolean
}
