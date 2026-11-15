package com.santacut.application.result

import com.santacut.domain.model.Participant
import com.santacut.domain.model.ParticipantSession
import java.time.Duration
import java.util.UUID

/**
 * 새로 등록한 참가자와 쿠키로만 내보낼 세션 토큰.
 *
 * @property sessionLifetime 세션 유효 기간. 세션 쿠키의 유효 기간으로 쓴다.
 */
data class RegisteredParticipantResult(
    val id: UUID,
    val nickname: String,
    val regionCode: String,
    val sessionToken: String,
    val sessionLifetime: Duration,
) {
    companion object {
        fun from(participant: Participant, sessionToken: String): RegisteredParticipantResult =
            RegisteredParticipantResult(
                participant.participantId(), participant.nickname, participant.regionCode, sessionToken,
                ParticipantSession.LIFETIME,
            )
    }
}
