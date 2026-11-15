package com.santacut.presentation.participant.controller

import com.santacut.IntegrationTest
import com.santacut.TestParticipants.REGION_CODE
import com.santacut.TestParticipants.uniqueNickname
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.assertTrue

/**
 * HTTP 경로, 상태 코드, 세션 쿠키, CSRF, 오류 응답 본문이 기존 계약대로인지 확인한다.
 */
@IntegrationTest
class ParticipantApiTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {
    @Test
    fun `CSRF 토큰을 받으면 헤더 이름과 토큰을 돌려준다`() {
        mockMvc.perform(get("/v1/csrf"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
            .andExpect(jsonPath("$.token").isString)
    }

    @Test
    fun `등록하면 201과 HttpOnly 세션 쿠키를 받고 그 쿠키로 내 정보를 본다`() {
        val nickname = uniqueNickname()
        val token = registerAndReadSessionToken(nickname)
        mockMvc.perform(get("/v1/me").cookie(Cookie(SESSION_COOKIE, token)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.nickname").value(nickname))
            .andExpect(jsonPath("$.regionCode").value(REGION_CODE))
    }

    @Test
    fun `이미 쓰는 닉네임으로 등록하면 409와 NICKNAME_TAKEN이다`() {
        val nickname = uniqueNickname()
        registerAndReadSessionToken(nickname)
        mockMvc.perform(registerRequest(nickname.uppercase()).with(csrf()))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("NICKNAME_TAKEN"))
    }

    /** 등록 응답의 Set-Cookie 속성을 확인하고 세션 토큰 원문을 꺼낸다. */
    private fun registerAndReadSessionToken(nickname: String): String {
        val setCookie = mockMvc.perform(registerRequest(nickname).with(csrf()))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.nickname").value(nickname))
            .andExpect(jsonPath("$.regionCode").value(REGION_CODE))
            .andReturn().response.getHeader(HttpHeaders.SET_COOKIE).orEmpty()
        assertTrue(setCookie.startsWith("$SESSION_COOKIE="))
        assertTrue(setCookie.contains("HttpOnly") && setCookie.contains("SameSite=Strict") && setCookie.contains("Path=/"))
        return setCookie.substringAfter("$SESSION_COOKIE=").substringBefore(";")
    }

    @Test
    fun `CSRF 토큰 없이 등록하면 403이다`() {
        mockMvc.perform(registerRequest(uniqueNickname())).andExpect(status().isForbidden)
    }

    @Test
    fun `세션 쿠키 없이 로그인이 필요한 경로를 부르면 401이다`() {
        mockMvc.perform(get("/v1/me")).andExpect(status().isUnauthorized)
        mockMvc.perform(get("/v1/rankings")).andExpect(status().isUnauthorized)
    }

    @Test
    fun `비즈니스 오류는 기존 필드에 오류 코드와 한국어 메시지를 더해 돌려준다`() {
        mockMvc.perform(registerRequest("!").with(csrf()))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.path").value("/v1/participants"))
            .andExpect(jsonPath("$.timestamp").isString)
            .andExpect(jsonPath("$.code").value("INVALID_NICKNAME"))
            .andExpect(jsonPath("$.message").isString)
    }

    @Test
    fun `형식이 틀린 공유 토큰의 공개 페이지는 404다`() {
        mockMvc.perform(get("/s/not-a-token"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("SHARE_NOT_FOUND"))
    }

    private fun registerRequest(nickname: String) = post("/v1/participants")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"nickname\":\"$nickname\",\"regionCode\":\"$REGION_CODE\"}")

    companion object {
        private const val SESSION_COOKIE = "sc_session"
    }
}
