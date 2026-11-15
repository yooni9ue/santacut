package com.santacut.presentation.global.security

import com.santacut.application.session.command.SessionAuthenticationCommand
import com.santacut.application.session.facade.SessionFacade
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

/**
 * 세션 쿠키로 참가자를 인증한다. 유효한 세션이면 참가자 UUID를 인증 주체로 넣는다.
 *
 * 필터는 HTTP 진입점이므로 controller와 같이 facade를 거쳐 세션을 확인한다.
 */
class SessionFilter(
    private val sessionFacade: SessionFacade,
    private val cookieName: String,
) : OncePerRequestFilter() {
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        findSessionToken(request)?.let(::authenticate)
        chain.doFilter(request, response)
    }

    private fun findSessionToken(request: HttpServletRequest): String? =
        RequestCookies.of(request)
            .valueOf(cookieName)
            ?.takeIf { it.length in SessionCookie.TOKEN_LENGTH_RANGE }

    private fun authenticate(token: String) {
        val session = sessionFacade.authenticate(SessionAuthenticationCommand(token)) ?: return
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(session.participantId, null, emptyList())
    }
}
