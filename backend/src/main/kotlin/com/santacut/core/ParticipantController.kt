package com.santacut.core

import com.santacut.api.docs.GetCsrfApi
import com.santacut.api.docs.GetCurrentParticipantApi
import com.santacut.api.docs.ListRegionsApi
import com.santacut.api.docs.RegisterParticipantApi
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.security.core.Authentication
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.security.SecureRandom
import java.text.Normalizer
import java.time.Instant
import java.util.Base64
import java.util.Locale
import java.util.UUID

fun randomToken(): String = ByteArray(32).also { SecureRandom().nextBytes(it) }.let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }
data class CsrfResponse(
    @field:Schema(description = "Request header name to send, usually X-XSRF-TOKEN", example = "X-XSRF-TOKEN") val headerName: String,
    @field:Schema(description = "CSRF token value; echo in headerName and retain the XSRF-TOKEN cookie", example = "e9d3...f61a") val token: String
)
data class RegionResponse(
    @field:Schema(description = "Stable region code used for registration", example = "KR-11-650") val code: String,
    @field:Schema(description = "Korean display name", example = "영등포구") val name: String
)
data class ParticipantRequest(
    @field:Schema(description = "Unique nickname. NFKC-normalized; 2–16 Korean/Latin letters, digits or underscores.", example = "산타도와줘", minLength = 2, maxLength = 16) val nickname: String,
    @field:Schema(description = "One of the region codes returned by GET /v1/regions", example = "KR-11-650") val regionCode: String
)
data class ParticipantResponse(
    @field:Schema(description = "New participant UUID", format = "uuid", example = "7af6f50a-4a88-4c32-9be5-a210a8ce1234") val id: UUID,
    @field:Schema(description = "Canonical nickname", example = "산타도와줘") val nickname: String,
    @field:Schema(description = "Registered region code", example = "KR-11-650") val regionCode: String
)
data class CurrentParticipantResponse(
    @field:Schema(description = "Authenticated participant UUID", format = "uuid") val id: UUID,
    @field:Schema(description = "Registered nickname") val nickname: String,
    @field:Schema(description = "Registered region code", example = "KR-11-650") val regionCode: String
)

@RestController
class ParticipantController(
    private val participants: ParticipantRepository,
    private val regionsRepository: RegionRepository,
    private val sessions: SessionRepository,
    @Value("\${app.session-secure}") private val secure: Boolean
) {
    @GetMapping("/v1/csrf")
    @GetCsrfApi
    fun csrf(@Parameter(hidden = true) token: CsrfToken) = CsrfResponse(token.headerName, token.token)

    @GetMapping("/v1/regions")
    @ListRegionsApi
    fun regions() = regionsRepository.findAllByOrderByCodeAsc().map { RegionResponse(it.code, it.name) }

    @GetMapping("/v1/me")
    @GetCurrentParticipantApi
    fun me(authentication: Authentication): CurrentParticipantResponse {
        val id = authentication.principal as UUID
        val participant = participants.findById(id.toString()).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }
        return CurrentParticipantResponse(id, participant.nickname, participant.regionCode)
    }

    @PostMapping("/v1/participants")
    @RegisterParticipantApi
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    fun create(@RequestBody input: ParticipantRequest, response: HttpServletResponse): ParticipantResponse {
        val nickname = Normalizer.normalize(input.nickname.trim(), Normalizer.Form.NFKC)
        if (!nickname.matches(Regex("[가-힣A-Za-z0-9_]{2,16}"))) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Nickname must contain 2-16 Korean/Latin letters, digits or underscore")
        if (!regionsRepository.existsById(input.regionCode)) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported region")
        val id = UUID.randomUUID()
        try {
            participants.saveAndFlush(ParticipantEntity(id.toString(), nickname, nickname.lowercase(Locale.ROOT), input.regionCode, utcDatabaseTime(Instant.now())))
        } catch (exception: DataIntegrityViolationException) {
            if (exception.mostSpecificCause.message?.contains("participants.nickname_key") == true) {
                throw ResponseStatusException(HttpStatus.CONFLICT, "Nickname already taken")
            }
            throw exception
        }
        val token = randomToken()
        sessions.save(SessionEntity(tokenHash(token), id.toString(), utcDatabaseTime(Instant.now().plusSeconds(2592000))))
        response.addHeader("Set-Cookie", ResponseCookie.from(if (secure) "__Host-sc_session" else "sc_session", token).httpOnly(true).secure(secure).sameSite("Strict").path("/").maxAge(2592000).build().toString())
        return ParticipantResponse(id, nickname, input.regionCode)
    }
}
