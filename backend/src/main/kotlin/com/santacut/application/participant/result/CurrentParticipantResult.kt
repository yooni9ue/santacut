package com.santacut.application.participant.result

import com.santacut.domain.participant.model.Participant
import java.util.UUID

/**
 * 로그인한 참가자 정보.
 */
data class CurrentParticipantResult(
    val id: UUID,
    val nickname: String,
    val regionCode: String,
) {
    companion object {
        fun from(participant: Participant): CurrentParticipantResult =
            CurrentParticipantResult(participant.participantId(), participant.nickname, participant.regionCode)
    }
}
