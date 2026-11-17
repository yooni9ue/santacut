package com.santacut.presentation.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 웹 계층이 읽는 세션·CSRF 쿠키 설정.
 *
 * @property sessionSecure 참이면 쿠키에 Secure 속성과 `__Host-` 이름을 쓴다
 * @property sessionSameSite 세션·CSRF 쿠키의 SameSite 값(Strict, Lax, None). None은 Secure일 때만 쓸 수 있다
 * @property requireSecureSession 참이면(운영 프로필) [sessionSecure]가 거짓일 때 앱이 뜨지 않는다
 */
@ConfigurationProperties(prefix = "app")
data class SessionCookieProperties(
    val sessionSecure: Boolean,
    val sessionSameSite: String = LAX,
    val requireSecureSession: Boolean = false,
) {
    init {
        require(sessionSameSite in SAME_SITE_VALUES) { "APP_SESSION_SAME_SITE는 Strict, Lax, None 가운데 하나여야 합니다." }
        require(sessionSameSite != NONE || sessionSecure) { "APP_SESSION_SAME_SITE=None은 APP_SESSION_SECURE=true일 때만 쓸 수 있습니다." }
        require(sessionSecure || !requireSecureSession) { "운영 프로필에서는 SESSION_SECURE(APP_SESSION_SECURE)가 true여야 합니다." }
    }

    companion object {
        private const val LAX = "Lax"
        private const val NONE = "None"
        private val SAME_SITE_VALUES = setOf("Strict", LAX, NONE)
    }
}
