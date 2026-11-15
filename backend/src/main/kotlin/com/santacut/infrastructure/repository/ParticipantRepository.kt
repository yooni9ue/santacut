package com.santacut.infrastructure.repository

import com.santacut.domain.model.Participant
import org.springframework.data.jpa.repository.JpaRepository

interface ParticipantRepository : JpaRepository<Participant, String>
