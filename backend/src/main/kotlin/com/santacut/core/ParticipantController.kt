package com.santacut.core

import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.security.web.csrf.CsrfToken
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
data class ParticipantRequest(val nickname: String, val regionCode: String)

@RestController
class ParticipantController(
    private val participants: ParticipantRepository,
    private val regionsRepository: RegionRepository,
    private val sessions: SessionRepository,
    @Value("\${app.session-secure}") private val secure: Boolean
) {
    @GetMapping("/v1/csrf")
    fun csrf(token: CsrfToken) = mapOf("headerName" to token.headerName, "token" to token.token)

    @GetMapping("/v1/regions")
    fun regions() = regionsRepository.findAllByOrderByCodeAsc().map { mapOf("code" to it.code, "name" to it.name) }

    @PostMapping("/v1/participants")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    fun create(@RequestBody input: ParticipantRequest, response: HttpServletResponse): Map<String, Any> {
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
        return mapOf("id" to id, "nickname" to nickname, "regionCode" to input.regionCode)
    }
}
