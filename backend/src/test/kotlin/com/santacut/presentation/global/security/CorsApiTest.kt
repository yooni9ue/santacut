package com.santacut.presentation.global.security

import com.santacut.IntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 허용한 프론트엔드 출처만 쿠키를 실은 CORS 요청을 열 수 있는지 확인한다.
 */
@IntegrationTest
class CorsApiTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {
    @Test
    fun `허용한 출처의 사전 요청은 자격 증명과 CSRF 헤더를 허용받는다`() {
        mockMvc.perform(preflight(ALLOWED_ORIGIN))
            .andExpect(status().isOk)
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
            .andExpect(header().stringValues(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, CSRF_HEADER))
    }

    @Test
    fun `허용하지 않은 출처의 사전 요청은 403이고 CORS 헤더가 없다`() {
        mockMvc.perform(preflight(DISALLOWED_ORIGIN))
            .andExpect(status().isForbidden)
            .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
            .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS))
    }

    private fun preflight(origin: String) = options("/v1/participants")
        .header(HttpHeaders.ORIGIN, origin)
        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, CSRF_HEADER)

    companion object {
        private const val ALLOWED_ORIGIN = "https://www.santacut.me"
        private const val DISALLOWED_ORIGIN = "https://evil.example"
        private const val CSRF_HEADER = "X-XSRF-TOKEN"
    }
}
