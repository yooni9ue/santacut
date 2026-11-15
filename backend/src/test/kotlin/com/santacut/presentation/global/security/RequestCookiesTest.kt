package com.santacut.presentation.global.security

import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RequestCookiesTest {
    @Test
    fun `이름이 같은 첫 쿠키의 값을 돌려준다`() {
        val cookies = RequestCookies(
            listOf(Cookie("other", "x"), Cookie("sc_session", "first"), Cookie("sc_session", "second")),
        )
        assertEquals("first", cookies.valueOf("sc_session"))
    }

    @Test
    fun `쿠키가 없으면 null이다`() {
        assertNull(RequestCookies(emptyList()).valueOf("sc_session"))
    }
}
