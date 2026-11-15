package com.santacut.application.participant.command

import java.util.UUID

/**
 * 로그인한 참가자의 지역을 바꾼다.
 */
data class ChangeRegionCommand(
    val participantId: UUID,
    val regionCode: String,
)
