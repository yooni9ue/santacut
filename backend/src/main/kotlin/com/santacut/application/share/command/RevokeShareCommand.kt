package com.santacut.application.share.command

import java.util.UUID

/**
 * 참가자 본인의 공유 링크를 취소한다.
 */
data class RevokeShareCommand(
    val participantId: UUID,
    val token: String,
)
