package com.santacut.application.participant.facade

import com.santacut.IntegrationTest
import com.santacut.application.participant.command.ListRegionsCommand
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * schema.sql이 채운 서울 데이터가 앱 시작 때 시·도 → 자치구 계층으로 이어지는지 확인한다.
 * HTTP 계약은 presentation의 RegionApiTest가 확인한다.
 */
@IntegrationTest
class RegionFacadeTest @Autowired constructor(
    private val regionFacade: RegionFacade,
) {
    @Test
    fun `앱이 뜨면 서울 자치구 25개가 모두 서울특별시 아래로 이어진다`() {
        val districts = regionFacade.listRegions(ListRegionsCommand(SEOUL)).map { it.parentCode }
        assertEquals(List(SEOUL_DISTRICT_COUNT) { SEOUL }, districts)
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
        private const val SEOUL = "KR-11"
        private const val SEOUL_DISTRICT_COUNT = 25
    }
}
