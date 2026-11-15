package com.santacut.application.command

import java.util.UUID

/**
 * 로그인한 참가자 정보를 조회한다.
 */
data class CurrentParticipantCommand(
    val participantId: UUID,
)
