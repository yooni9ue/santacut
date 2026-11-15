package com.santacut.application.command

import java.util.UUID

/**
 * 로그인한 참가자의 최신 결과를 조회한다.
 */
data class MyResultCommand(
    val participantId: UUID,
)
