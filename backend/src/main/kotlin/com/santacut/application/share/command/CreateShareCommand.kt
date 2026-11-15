package com.santacut.application.share.command

import java.util.UUID

/**
 * 로그인한 참가자의 최신 결과로 공유 링크를 만든다.
 */
data class CreateShareCommand(
    val participantId: UUID,
)
