package com.santacut.presentation.prompt.controller

import com.santacut.IntegrationTest
import com.santacut.TestCsrf.withCsrf
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

/**
 * 켜진 프롬프트 관리 API(dev)의 경로, 상태 코드, JSON 필드, 검증, CSRF를 확인한다.
 */
@IntegrationTest
class PromptAdminApiTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {
    @Test
    fun `새 판 저장은 201과 이름, 판 번호, 저장 시각을 돌려주고 저장할 때마다 판 번호가 는다`() {
        val name = uniqueName()

        create(name, "1판")
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value(name))
            .andExpect(jsonPath("$.version").value(1))
            .andExpect(jsonPath("$.created_at").exists())
            .andExpect(jsonPath("$.content").doesNotExist())
        create(name, "2판")
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.version").value(2))
    }

    @Test
    fun `최신 판 조회는 가장 높은 판의 본문을 돌려준다`() {
        val name = uniqueName()
        create(name, "1판")
        create(name, "2판")

        mockMvc.perform(get(PATH, name))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value(name))
            .andExpect(jsonPath("$.version").value(2))
            .andExpect(jsonPath("$.content").value("2판"))
            .andExpect(jsonPath("$.created_at").exists())
    }

    @Test
    fun `판 목록은 본문 없이 높은 판부터 배열로 나온다`() {
        val name = uniqueName()
        create(name, "1판")
        create(name, "2판")

        mockMvc.perform(get("$PATH/versions", name))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].version").value(2))
            .andExpect(jsonPath("$[1].version").value(1))
            .andExpect(jsonPath("$[0].name").value(name))
            .andExpect(jsonPath("$[0].created_at").exists())
            .andExpect(jsonPath("$[0].content").doesNotExist())
    }

    @Test
    fun `없는 이름은 PROMPT_NOT_FOUND 404다`() {
        val name = uniqueName()

        mockMvc.perform(get(PATH, name))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("PROMPT_NOT_FOUND"))
        mockMvc.perform(get("$PATH/versions", name))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("PROMPT_NOT_FOUND"))
    }

    @Test
    fun `이름이 규칙을 벗어나면 INVALID_REQUEST 400이다`() {
        create("Score", "본문").andExpect(status().isBadRequest).andExpect(jsonPath("$.code").value(INVALID_REQUEST))
        create("a".repeat(MAX_NAME_LENGTH + 1), "본문").andExpect(status().isBadRequest)
        mockMvc.perform(get(PATH, "score_v2"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value(INVALID_REQUEST))
    }

    @Test
    fun `본문이 비었거나 20,000자를 넘거나 없으면 INVALID_REQUEST 400이다`() {
        val name = uniqueName()

        create(name, "").andExpect(status().isBadRequest).andExpect(jsonPath("$.code").value(INVALID_REQUEST))
        create(name, "가".repeat(MAX_CONTENT_LENGTH + 1)).andExpect(status().isBadRequest)
        mockMvc.perform(withCsrf(mockMvc, post(PATH, name)).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value(INVALID_REQUEST))
        mockMvc.perform(get(PATH, name)).andExpect(status().isNotFound)
    }

    @Test
    fun `본문이 정확히 20,000자면 저장한다`() {
        create(uniqueName(), "가".repeat(MAX_CONTENT_LENGTH)).andExpect(status().isCreated)
    }

    @Test
    fun `CSRF 토큰 없이 새 판을 저장하면 403이고 저장하지 않는다`() {
        val name = uniqueName()

        mockMvc.perform(post(PATH, name).contentType(MediaType.APPLICATION_JSON).content(body("본문")))
            .andExpect(status().isForbidden)
        mockMvc.perform(get(PATH, name)).andExpect(status().isNotFound)
    }

    private fun create(name: String, content: String): ResultActions = mockMvc.perform(
        withCsrf(mockMvc, post(PATH, name)).contentType(MediaType.APPLICATION_JSON).content(body(content)),
    )

    private fun body(content: String): String = """{"content":"$content"}"""

    private fun uniqueName(): String = "api-" + UUID.randomUUID().toString().take(NAME_SUFFIX_LENGTH)

    companion object {
        private const val PATH = "/v1/admin/prompts/{name}"
        private const val INVALID_REQUEST = "INVALID_REQUEST"
        private const val MAX_NAME_LENGTH = 40
        private const val MAX_CONTENT_LENGTH = 20_000
        private const val NAME_SUFFIX_LENGTH = 8
    }
}
