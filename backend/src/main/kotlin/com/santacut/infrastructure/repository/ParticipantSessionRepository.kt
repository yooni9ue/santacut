package com.santacut.infrastructure.repository

import com.santacut.domain.model.ParticipantSession
import org.springframework.data.jpa.repository.JpaRepository

interface ParticipantSessionRepository : JpaRepository<ParticipantSession, String>
