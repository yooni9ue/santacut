package com.santacut.application.participant.command

import java.util.UUID

/**
 * 로그인한 참가자와 그 참가자의 모든 데이터를 지운다.
 */
data class DeleteParticipantCommand(
    val participantId: UUID,
)
