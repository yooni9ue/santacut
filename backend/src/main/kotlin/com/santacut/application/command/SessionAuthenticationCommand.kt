package com.santacut.application.command

/**
 * 세션 쿠키 토큰으로 참가자를 인증한다.
 */
data class SessionAuthenticationCommand(
    val token: String,
)
