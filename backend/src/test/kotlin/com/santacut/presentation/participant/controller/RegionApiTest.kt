package com.santacut.presentation.participant.controller

import com.santacut.IntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * GET /v1/regions의 경로, 쿼리, 상태 코드, JSON 필드가 기존 계약대로인지 확인한다.
 */
@IntegrationTest
class RegionApiTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {
    @Test
    fun `상위 지역 없이 부르면 고를 수 없는 서울특별시 하나만 나온다`() {
        mockMvc.perform(get(REGIONS_PATH))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].code").value(SEOUL))
            .andExpect(jsonPath("$[0].name").value("서울특별시"))
            .andExpect(jsonPath("$[0].parent_code").doesNotExist())
            .andExpect(jsonPath("$[0].selectable").value(false))
    }

    @Test
    fun `서울특별시 아래에는 고를 수 있는 25개 자치구가 코드 순서로 나온다`() {
        mockMvc.perform(get(REGIONS_PATH).param("parent", SEOUL))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(SEOUL_DISTRICT_COUNT))
            .andExpect(jsonPath("$[0].code").value("KR-11-110"))
            .andExpect(jsonPath("$[0].parent_code").value(SEOUL))
            .andExpect(jsonPath("$[0].selectable").value(true))
    }

    @Test
    fun `모르는 상위 지역이면 200과 빈 배열이다`() {
        mockMvc.perform(get(REGIONS_PATH).param("parent", "KR-99"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    companion object {
        private const val REGIONS_PATH = "/v1/regions"
        private const val SEOUL = "KR-11"
        private const val SEOUL_DISTRICT_COUNT = 25
    }
}
