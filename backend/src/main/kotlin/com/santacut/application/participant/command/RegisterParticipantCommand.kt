package com.santacut.application.participant.command

/**
 * 참가자를 등록하고 세션을 연다.
 */
data class RegisterParticipantCommand(
    val nickname: String,
    val regionCode: String,
)
