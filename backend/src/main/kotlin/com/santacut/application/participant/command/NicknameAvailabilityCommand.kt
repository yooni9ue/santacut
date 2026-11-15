package com.santacut.application.participant.command

/**
 * 등록 전에 닉네임을 쓸 수 있는지 확인한다.
 */
data class NicknameAvailabilityCommand(
    val nickname: String,
)
