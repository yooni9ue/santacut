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
    fun `상위 지역 없이 부르면 시·도 16개가 코드 순서로 나오고 서울특별시는 고를 수 없다`() {
        mockMvc.perform(get(REGIONS_PATH))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(PROVINCE_COUNT))
            .andExpect(jsonPath("$[0].code").value(SEOUL))
            .andExpect(jsonPath("$[0].name").value("서울특별시"))
            .andExpect(jsonPath("$[0].parent_code").doesNotExist())
            .andExpect(jsonPath("$[0].selectable").value(false))
            .andExpect(jsonPath("$[1].code").value("KR-12"))
            .andExpect(jsonPath("$[1].name").value("전남광주통합특별시"))
    }

    @Test
    fun `하위 지역이 없는 세종특별자치시는 시·도 목록에서 바로 고를 수 있다`() {
        mockMvc.perform(get(REGIONS_PATH))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[?(@.code == 'KR-36')].name").value("세종특별자치시"))
            .andExpect(jsonPath("$[?(@.code == 'KR-36')].selectable").value(true))
        mockMvc.perform(get(REGIONS_PATH).param("parent", "KR-36"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `경기도 아래에는 일반구를 시 이름과 띄워 쓴 47개 지역이 나온다`() {
        mockMvc.perform(get(REGIONS_PATH).param("parent", "KR-41"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(GYEONGGI_CHILD_COUNT))
            .andExpect(jsonPath("$[0].code").value("KR-41-111"))
            .andExpect(jsonPath("$[0].name").value("수원시 장안구"))
            .andExpect(jsonPath("$[0].parent_code").value("KR-41"))
            .andExpect(jsonPath("$[0].selectable").value(true))
    }

    @Test
    fun `이름이 같은 중구도 상위 지역마다 다른 코드로 나온다`() {
        mockMvc.perform(get(REGIONS_PATH).param("parent", "KR-26"))
            .andExpect(jsonPath("$[0].code").value("KR-26-110"))
            .andExpect(jsonPath("$[0].name").value("중구"))
            .andExpect(jsonPath("$[0].parent_code").value("KR-26"))
        mockMvc.perform(get(REGIONS_PATH).param("parent", "KR-27"))
            .andExpect(jsonPath("$[0].code").value("KR-27-110"))
            .andExpect(jsonPath("$[0].name").value("중구"))
            .andExpect(jsonPath("$[0].parent_code").value("KR-27"))
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
        private const val PROVINCE_COUNT = 16
        private const val GYEONGGI_CHILD_COUNT = 47
    }
}
