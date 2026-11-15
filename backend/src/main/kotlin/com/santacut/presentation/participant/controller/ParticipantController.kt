package com.santacut.presentation.participant.controller

import com.santacut.application.participant.command.CurrentParticipantCommand
import com.santacut.application.participant.facade.ParticipantFacade
import com.santacut.application.participant.result.RegisteredParticipantResult
import com.santacut.presentation.global.config.SessionCookieProperties
import com.santacut.presentation.global.security.SessionCookie
import com.santacut.presentation.participant.docs.GetCurrentParticipantApi
import com.santacut.presentation.participant.docs.RegisterParticipantApi
import com.santacut.presentation.participant.request.RegisterParticipantRequest
import com.santacut.presentation.participant.response.CurrentParticipantResponse
import com.santacut.presentation.participant.response.ParticipantResponse
import io.swagger.v3.oas.annotations.Parameter
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class ParticipantController(
    private val participantFacade: ParticipantFacade,
    private val sessionCookieProperties: SessionCookieProperties,
) {
    @GetMapping("/v1/me")
    @GetCurrentParticipantApi
    fun me(@AuthenticationPrincipal @Parameter(hidden = true) participantId: UUID): CurrentParticipantResponse =
        CurrentParticipantResponse.from(participantFacade.getCurrentParticipant(CurrentParticipantCommand(participantId)))

    /** 세션 토큰은 JSON 본문에 넣지 않고 HttpOnly 쿠키로만 내려보낸다. */
    @PostMapping("/v1/participants")
    @RegisterParticipantApi
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@RequestBody request: RegisterParticipantRequest, response: HttpServletResponse): ParticipantResponse {
        val result = participantFacade.register(request.toCommand())
        response.addHeader(HttpHeaders.SET_COOKIE, sessionCookie(result).toString())
        return ParticipantResponse.from(result)
    }

    private fun sessionCookie(result: RegisteredParticipantResult): ResponseCookie =
        ResponseCookie.from(SessionCookie.name(sessionCookieProperties.sessionSecure), result.sessionToken)
            .httpOnly(true)
            .secure(sessionCookieProperties.sessionSecure)
            .sameSite(SAME_SITE)
            .path(ROOT_PATH)
            .maxAge(result.sessionLifetime)
            .build()

    companion object {
        private const val SAME_SITE = "Strict"
        private const val ROOT_PATH = "/"
    }
}
