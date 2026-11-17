package com.santacut.presentation.global.docs

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 운영 프로필은 SWAGGER_ENABLED를 켜지 않으면 OpenAPI 명세와 Swagger UI를 내놓지 않는다(404).
 * 설정이 다른 컨텍스트라 `build/swagger-disabled-test.db`를 따로 쓴다.
 */
@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:sqlite:./build/swagger-disabled-test.db",
        "app.season-year=2099",
        "santacut.ai.poll-delay-ms=3600000",
        "santacut.upload.poll-delay-ms=3600000",
    ],
)
@ActiveProfiles("production")
@AutoConfigureMockMvc
class SwaggerDisabledInProductionApiTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {
    @Test
    fun `운영 프로필 기본값에서는 OpenAPI 명세와 Swagger UI가 404다`() {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound)
        mockMvc.perform(get("/swagger-ui.html")).andExpect(status().isNotFound)
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isNotFound)
    }
}
