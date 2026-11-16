package com.santacut.presentation.participant.controller

import com.santacut.IntegrationTest
import com.santacut.TestCsrf.withCsrf
import com.santacut.TestParticipants.REGION_CODE
import com.santacut.TestParticipants.uniqueNickname
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
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
        mockMvc.perform(withCsrf(mockMvc, registerRequest(nickname.uppercase())))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("NICKNAME_TAKEN"))
    }

    /** 등록 응답의 Set-Cookie 속성을 확인하고 세션 토큰 원문을 꺼낸다. */
    private fun registerAndReadSessionToken(nickname: String): String {
        val setCookie = mockMvc.perform(withCsrf(mockMvc, registerRequest(nickname)))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.nickname").value(nickname))
            .andExpect(jsonPath("$.regionCode").value(REGION_CODE))
            .andReturn().response.getHeader(HttpHeaders.SET_COOKIE).orEmpty()
        assertTrue(setCookie.startsWith("$SESSION_COOKIE="))
        assertTrue(setCookie.contains("HttpOnly") && setCookie.contains("SameSite=Lax") && setCookie.contains("Path=/"))
        return setCookie.substringAfter("$SESSION_COOKIE=").substringBefore(";")
    }

    @Test
    fun `닉네임 가용성은 로그인 없이 확인하고 형식 오류는 INVALID_NICKNAME이다`() {
        val nickname = uniqueNickname()
        mockMvc.perform(get(AVAILABILITY_PATH).param("nickname", nickname))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.available").value(true))
        registerAndReadSessionToken(nickname)
        mockMvc.perform(get(AVAILABILITY_PATH).param("nickname", nickname))
            .andExpect(jsonPath("$.available").value(false))
        mockMvc.perform(get(AVAILABILITY_PATH).param("nickname", "!"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_NICKNAME"))
        mockMvc.perform(get(AVAILABILITY_PATH))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
    }

    @Test
    fun `지역 변경은 CSRF 토큰과 세션이 있어야 하고 바뀐 참가 정보를 돌려준다`() {
        val session = Cookie(SESSION_COOKIE, registerAndReadSessionToken(uniqueNickname()))
        val change = patch(REGION_PATH).cookie(session).contentType(MediaType.APPLICATION_JSON)
            .content("{\"regionCode\":\"$OTHER_REGION_CODE\"}")
        mockMvc.perform(change).andExpect(status().isForbidden)
        mockMvc.perform(withCsrf(mockMvc, change))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.regionCode").value(OTHER_REGION_CODE))
        mockMvc.perform(withCsrf(mockMvc, patch(REGION_PATH).contentType(MediaType.APPLICATION_JSON).content("{}")))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `내 데이터를 지우면 204와 만료된 세션 쿠키를 받고 그 세션은 더 이상 인증되지 않는다`() {
        val session = Cookie(SESSION_COOKIE, registerAndReadSessionToken(uniqueNickname()))
        mockMvc.perform(delete(ME_PATH).cookie(session)).andExpect(status().isForbidden)
        val setCookie = mockMvc.perform(withCsrf(mockMvc, delete(ME_PATH).cookie(session)))
            .andExpect(status().isNoContent)
            .andReturn().response.getHeader(HttpHeaders.SET_COOKIE).orEmpty()
        assertTrue(setCookie.startsWith("$SESSION_COOKIE=;") && setCookie.contains("Max-Age=0"))
        mockMvc.perform(get(ME_PATH).cookie(session)).andExpect(status().isUnauthorized)
    }

    @Test
    fun `CSRF 토큰 없이 등록하면 403이다`() {
        mockMvc.perform(registerRequest(uniqueNickname())).andExpect(status().isForbidden)
    }

    @Test
    fun `세션 쿠키 없이 로그인이 필요한 경로를 부르면 401이다`() {
        mockMvc.perform(get("/v1/me")).andExpect(status().isUnauthorized)
        mockMvc.perform(get("/v1/me/result")).andExpect(status().isUnauthorized)
    }

    @Test
    fun `순위표는 로그인 없이 메타 정보와 함께 본다`() {
        mockMvc.perform(get("/v1/rankings"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.season_year").value(2099))
            .andExpect(jsonPath("$.participant_count").isNumber)
            .andExpect(jsonPath("$.stage").value("PREVIEW"))
            .andExpect(jsonPath("$.rows").isArray)
    }

    @Test
    fun `이전 응답이 준 값이 아닌 순위표 cursor는 400과 INVALID_CURSOR다`() {
        mockMvc.perform(get("/v1/rankings").param("cursor", "abc"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_CURSOR"))
    }

    @Test
    fun `비즈니스 오류는 기존 필드에 오류 코드와 한국어 메시지를 더해 돌려준다`() {
        mockMvc.perform(withCsrf(mockMvc, registerRequest("!")))
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

    @Test
    fun `브라우저가 연 없는 공유 페이지는 404와 볼 수 없는 결과 HTML이다`() {
        val html = mockMvc.perform(get("/s/not-a-token").accept(MediaType.TEXT_HTML))
            .andExpect(status().isNotFound)
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
            .andReturn().response.getContentAsString(Charsets.UTF_8)
        assertTrue(html.contains("더 이상 볼 수 없는 결과") && html.contains("내 산타 점수 알아보기"))
    }

    @Test
    fun `없는 공유 이미지도 Accept에 text-html이 있으면 404 HTML이고 없으면 404 JSON이다`() {
        mockMvc.perform(get(MISSING_SHARE_IMAGE_PATH).accept(MediaType.TEXT_HTML, MediaType.ALL))
            .andExpect(status().isNotFound)
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
        mockMvc.perform(get(MISSING_SHARE_IMAGE_PATH).accept(MediaType.IMAGE_PNG, MediaType.ALL))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("SHARE_NOT_FOUND"))
    }

    private fun registerRequest(nickname: String) = post("/v1/participants")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"nickname\":\"$nickname\",\"regionCode\":\"$REGION_CODE\"}")

    companion object {
        private const val SESSION_COOKIE = "sc_session"
        private const val AVAILABILITY_PATH = "/v1/nicknames/availability"
        private const val REGION_PATH = "/v1/me/region"
        private const val ME_PATH = "/v1/me"
        private const val OTHER_REGION_CODE = "KR-11-650"
        private const val MISSING_SHARE_IMAGE_PATH = "/s/not-a-token/image.png"
    }
}
