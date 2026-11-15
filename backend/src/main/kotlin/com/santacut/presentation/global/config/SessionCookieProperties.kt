package com.santacut.presentation.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 웹 계층이 읽는 세션·CSRF 쿠키 설정.
 *
 * @property sessionSecure 참이면 쿠키에 Secure 속성과 `__Host-` 이름을 쓴다
 */
@ConfigurationProperties(prefix = "app")
data class SessionCookieProperties(
    val sessionSecure: Boolean,
)
