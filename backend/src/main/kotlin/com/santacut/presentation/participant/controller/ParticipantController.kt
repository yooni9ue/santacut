package com.santacut.presentation.participant.controller

import com.santacut.application.participant.command.CurrentParticipantCommand
import com.santacut.application.participant.command.DeleteParticipantCommand
import com.santacut.application.participant.command.NicknameAvailabilityCommand
import com.santacut.application.participant.facade.ParticipantFacade
import com.santacut.presentation.global.config.SessionCookieProperties
import com.santacut.presentation.global.security.SessionCookie
import com.santacut.presentation.participant.docs.ChangeRegionApi
import com.santacut.presentation.participant.docs.CheckNicknameAvailabilityApi
import com.santacut.presentation.participant.docs.DeleteMyDataApi
import com.santacut.presentation.participant.docs.GetCurrentParticipantApi
import com.santacut.presentation.participant.docs.RegisterParticipantApi
import com.santacut.presentation.participant.request.ChangeRegionRequest
import com.santacut.presentation.participant.request.RegisterParticipantRequest
import com.santacut.presentation.participant.response.CurrentParticipantResponse
import com.santacut.presentation.participant.response.NicknameAvailabilityResponse
import com.santacut.presentation.participant.response.ParticipantResponse
import io.swagger.v3.oas.annotations.Parameter
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
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

    @PatchMapping("/v1/me/region")
    @ChangeRegionApi
    fun changeRegion(
        @RequestBody request: ChangeRegionRequest,
        @AuthenticationPrincipal @Parameter(hidden = true) participantId: UUID,
    ): CurrentParticipantResponse =
        CurrentParticipantResponse.from(participantFacade.changeRegion(request.toCommand(participantId)))

    /** 참가자를 지운 뒤 브라우저의 세션 쿠키도 바로 만료시킨다. */
    @DeleteMapping("/v1/me")
    @DeleteMyDataApi
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteMyData(@AuthenticationPrincipal @Parameter(hidden = true) participantId: UUID): ResponseEntity<Void> {
        participantFacade.deleteMyData(DeleteParticipantCommand(participantId))
        return ResponseEntity.noContent()
            .header(HttpHeaders.SET_COOKIE, sessionCookie(EXPIRED_TOKEN, Duration.ZERO).toString())
            .build()
    }

    @GetMapping("/v1/nicknames/availability")
    @CheckNicknameAvailabilityApi
    fun nicknameAvailability(@RequestParam nickname: String): NicknameAvailabilityResponse =
        NicknameAvailabilityResponse.from(participantFacade.checkNicknameAvailability(NicknameAvailabilityCommand(nickname)))

    /** 세션 토큰은 JSON 본문에 넣지 않고 HttpOnly 쿠키로만 내려보낸다. */
    @PostMapping("/v1/participants")
    @RegisterParticipantApi
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@RequestBody request: RegisterParticipantRequest, response: HttpServletResponse): ParticipantResponse {
        val result = participantFacade.register(request.toCommand())
        response.addHeader(HttpHeaders.SET_COOKIE, sessionCookie(result.sessionToken, result.sessionLifetime).toString())
        return ParticipantResponse.from(result)
    }

    private fun sessionCookie(token: String, lifetime: Duration): ResponseCookie =
        ResponseCookie.from(SessionCookie.name(sessionCookieProperties.sessionSecure), token)
            .httpOnly(true)
            .secure(sessionCookieProperties.sessionSecure)
            .sameSite(SAME_SITE)
            .path(ROOT_PATH)
            .maxAge(lifetime)
            .build()

    companion object {
        private const val SAME_SITE = "Strict"
        private const val ROOT_PATH = "/"

        /** 쿠키를 지울 때 넣는 빈 값. 브라우저는 Max-Age=0 쿠키를 바로 버린다. */
        private const val EXPIRED_TOKEN = ""
    }
}
