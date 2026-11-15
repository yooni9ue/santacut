package com.santacut.application.session.result

import com.santacut.domain.session.model.ParticipantSession
import java.util.UUID

/**
 * 유효한 세션의 참가자.
 */
data class AuthenticatedSessionResult(
    val participantId: UUID,
) {
    companion object {
        fun from(session: ParticipantSession): AuthenticatedSessionResult =
            AuthenticatedSessionResult(session.participantId())
    }
}
