package com.santacut.presentation.global.security

/**
 * 로그인 세션 쿠키 규칙. 쿠키 유효 기간은 세션 유효 기간을 따르며 facade 결과로 받는다.
 */
object SessionCookie {
    /** 쿠키 값으로 받아들이는 토큰 길이. */
    val TOKEN_LENGTH_RANGE = 32..128

    private const val SECURE_NAME = "__Host-sc_session"
    private const val LOCAL_NAME = "sc_session"

    fun name(secure: Boolean): String = if (secure) SECURE_NAME else LOCAL_NAME
}
