package com.santacut.infrastructure.session.repository

import com.santacut.domain.session.model.ParticipantSession
import org.springframework.data.jpa.repository.JpaRepository

interface ParticipantSessionRepository : JpaRepository<ParticipantSession, String>
