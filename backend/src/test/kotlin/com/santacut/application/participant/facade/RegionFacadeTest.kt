package com.santacut.application.participant.facade

import com.santacut.IntegrationTest
import com.santacut.application.participant.command.ListRegionsCommand
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * schema.sql이 채운 서울 데이터가 시·도 → 자치구 계층으로 이어져 GET /v1/regions로 나가는지 확인한다.
 */
@IntegrationTest
class RegionFacadeTest @Autowired constructor(
    private val regionFacade: RegionFacade,
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
    fun `모르는 상위 지역이면 빈 목록이다`() {
        val codes = regionFacade.listRegions(ListRegionsCommand("KR-99")).map { it.code }
        assertTrue(codes.isEmpty())
    }

    @Test
    fun `계층 잇기는 여러 번 실행해도 결과가 같다`() {
        regionFacade.linkRegionHierarchy()
        val districts = regionFacade.listRegions(ListRegionsCommand(SEOUL)).map { it.parentCode }
        assertEquals(List(SEOUL_DISTRICT_COUNT) { SEOUL }, districts)
    }

    companion object {
        private const val REGIONS_PATH = "/v1/regions"
        private const val SEOUL = "KR-11"
        private const val SEOUL_DISTRICT_COUNT = 25
    }
}
