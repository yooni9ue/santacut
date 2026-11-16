package com.santacut.presentation.prompt.controller

import com.santacut.TestCsrf.withCsrf
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 운영처럼 `santacut.prompt-admin.enabled`를 켜지 않으면 프롬프트 관리 API가 없어 모든 경로가 404인지 확인한다.
 * 설정이 다른 컨텍스트라 다른 통합 테스트와 섞이지 않게 `build/prompt-admin-disabled-test.db`를 따로 쓴다.
 */
@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:sqlite:./build/prompt-admin-disabled-test.db",
        "santacut.ai.poll-delay-ms=3600000",
        "santacut.upload.poll-delay-ms=3600000",
    ],
)
@AutoConfigureMockMvc
class PromptAdminDisabledApiTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {
    @Test
    fun `꺼져 있으면 최신 판 조회와 판 목록 조회가 404다`() {
        mockMvc.perform(get(PATH, "score")).andExpect(status().isNotFound)
        mockMvc.perform(get("$PATH/versions", "score")).andExpect(status().isNotFound)
    }

    @Test
    fun `꺼져 있으면 CSRF 토큰을 붙인 새 판 저장도 404다`() {
        mockMvc.perform(
            withCsrf(mockMvc, post(PATH, "score")).contentType(MediaType.APPLICATION_JSON).content("""{"content":"본문"}"""),
        ).andExpect(status().isNotFound)
    }

    companion object {
        private const val PATH = "/v1/admin/prompts/{name}"
    }
}
