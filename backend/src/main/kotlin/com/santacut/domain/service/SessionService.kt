package com.santacut.domain.service

import com.santacut.domain.model.Participant
import com.santacut.domain.model.ParticipantSession
import com.santacut.domain.model.RandomToken
import com.santacut.infrastructure.repository.ParticipantSessionRepository
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class SessionService(
    private val participantSessionRepository: ParticipantSessionRepository,
) {
    /** 새 세션을 열고 쿠키에 담을 토큰 원문을 돌려준다. */
    fun open(participant: Participant, openedAt: Instant): String {
        val token = RandomToken.generate()
        participantSessionRepository.save(ParticipantSession.open(token, participant, openedAt))
        return token
    }

    /** 토큰에 해당하는 만료되지 않은 세션. */
    fun findActive(token: String, now: Instant): ParticipantSession? =
        participantSessionRepository.findById(ParticipantSession.hashOf(token)).orElse(null)
            ?.takeIf { it.isActiveAt(now) }
}
