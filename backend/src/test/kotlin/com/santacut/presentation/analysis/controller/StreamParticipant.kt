package com.santacut.presentation.analysis.controller

import jakarta.servlet.http.Cookie
import java.util.UUID

/**
 * 진행 상황 스트림 테스트에서 쓰는 등록한 참가자와 그 세션 쿠키.
 */
data class StreamParticipant(
    val id: UUID,
    val session: Cookie,
)
