package com.santacut.presentation.global.config

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.assertFailsWith

/**
 * SameSite 설정이 잘못되면 앱이 뜨기 전에 막는지 확인한다.
 */
class SessionCookiePropertiesTest {
    @Test
    fun `SameSite=None은 Secure 없이 쓸 수 없다`() {
        assertFailsWith<IllegalArgumentException> { SessionCookieProperties(sessionSecure = false, sessionSameSite = "None") }
        assertDoesNotThrow { SessionCookieProperties(sessionSecure = true, sessionSameSite = "None") }
    }

    @Test
    fun `SameSite는 Strict, Lax, None만 받는다`() {
        assertFailsWith<IllegalArgumentException> { SessionCookieProperties(sessionSecure = true, sessionSameSite = "none") }
    }
}
