package com.santacut.presentation.global.security

import com.santacut.IntegrationTest
import com.santacut.TestCsrf.withCsrf
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 파일 업로드가 아닌 요청 본문은 2MB를 넘으면 읽지 않고 413 REQUEST_TOO_LARGE로 막는지 확인한다.
 */
@IntegrationTest
class RequestBodyLimitApiTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {
    @Test
    fun `2MB를 넘는 JSON 본문은 413 REQUEST_TOO_LARGE다`() {
        val body = """{"nickname":"${"가".repeat(OVERSIZED_CHARACTERS)}","regionCode":"KR-11-110"}"""

        mockMvc.perform(withCsrf(mockMvc, post("/v1/participants")).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isPayloadTooLarge)
            .andExpect(jsonPath("$.code").value("REQUEST_TOO_LARGE"))
    }

    @Test
    fun `2MB 이하 JSON 본문은 막지 않는다`() {
        mockMvc.perform(
            withCsrf(mockMvc, post("/v1/participants")).contentType(MediaType.APPLICATION_JSON)
                .content("""{"nickname":"bad nickname!","regionCode":"KR-11-110"}"""),
        ).andExpect(status().isBadRequest)
    }

    companion object {
        /** UTF-8로 3바이트인 글자 수. 2MB를 넘긴다. */
        private const val OVERSIZED_CHARACTERS = 750_000
    }
}
