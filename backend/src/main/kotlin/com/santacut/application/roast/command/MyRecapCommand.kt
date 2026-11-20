package com.santacut.application.roast.command

import java.util.UUID

/** 로그인한 참가자의 리캡 카드 덱 조회. */
data class MyRecapCommand(val participantId: UUID)
