package com.santacut.presentation.global.security

import com.santacut.IntegrationTest
import com.santacut.TestCsrf.withCsrf
import com.santacut.TestParticipants.REGION_CODE
import com.santacut.TestParticipants.uniqueNickname
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * dev처럼 SameSite=None과 Secure를 켜면 다른 사이트(localhost)에서도 세션·CSRF 쿠키를 실을 수 있는지 확인한다.
 */
@IntegrationTest
@TestPropertySource(properties = ["app.session-secure=true", "app.session-same-site=None"])
class CrossSiteCookieApiTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {
    @Test
    fun `CSRF 쿠키는 __Host- 이름에 SameSite=None과 Secure로 내려간다`() {
        // CSRF 저장소는 서블릿 Cookie 속성으로 SameSite를 넣는다. Tomcat은 이를 Set-Cookie에 쓰지만 MockMvc 헤더 문자열에는 빠지므로 속성을 본다.
        val cookie = checkNotNull(
            mockMvc.perform(get("/v1/csrf")).andExpect(status().isOk).andReturn().response.getCookie("__Host-XSRF-TOKEN"),
        )
        assertEquals("None", cookie.getAttribute("SameSite"))
        assertTrue(cookie.secure)
        assertEquals("/", cookie.path)
    }

    @Test
    fun `세션 쿠키는 SameSite=None과 Secure로 내려간다`() {
        val register = post("/v1/participants").contentType(MediaType.APPLICATION_JSON)
            .content("{\"nickname\":\"${uniqueNickname()}\",\"regionCode\":\"$REGION_CODE\"}")
        val setCookie = mockMvc.perform(withCsrf(mockMvc, register))
            .andExpect(status().isCreated)
            .andReturn().response.getHeader(HttpHeaders.SET_COOKIE).orEmpty()
        assertTrue(setCookie.startsWith("__Host-sc_session=") && setCookie.contains("SameSite=None") && setCookie.contains("Secure"))
    }
}
