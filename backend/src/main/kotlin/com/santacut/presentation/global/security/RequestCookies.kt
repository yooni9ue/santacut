package com.santacut.presentation.global.security

import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest

/**
 * 요청에 실려 온 쿠키 목록.
 */
class RequestCookies(private val values: List<Cookie>) {
    /** 이름이 같은 첫 쿠키의 값. 없으면 null이다. */
    fun valueOf(name: String): String? = values.firstOrNull { it.name == name }?.value

    companion object {
        fun of(request: HttpServletRequest): RequestCookies = RequestCookies(request.cookies?.asList().orEmpty())
    }
}
