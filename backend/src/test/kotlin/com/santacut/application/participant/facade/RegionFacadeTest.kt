package com.santacut.application.participant.facade

import com.santacut.IntegrationTest
import com.santacut.application.participant.command.ListRegionsCommand
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * schema.sql과 regions.sql이 채운 전국 지역이 앱 시작 때 시·도 → 시·군·구 계층으로 이어지는지 확인한다.
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
    fun `앱이 뜨면 시·도마다 시·군·구가 기준일 개수만큼 이어진다`() {
        val counts = EXPECTED_CHILD_COUNTS.mapValues { (parent, _) ->
            regionFacade.listRegions(ListRegionsCommand(parent)).map { it.parentCode }.count { it == parent }
        }
        assertEquals(EXPECTED_CHILD_COUNTS, counts)
    }

    @Test
    fun `시·도는 16개이고 세종특별자치시는 하위 지역 없이 그 자체로 고른다`() {
        val provinces = regionFacade.listRegions(ListRegionsCommand(null))
        val selectableByCode = provinces.map { it.code to it.selectable }.toMap()
        assertEquals(PROVINCE_COUNT, selectableByCode.size)
        assertEquals(true, selectableByCode[SEJONG])
        assertEquals(false, selectableByCode[SEOUL])
    }

    @Test
    fun `서울 자치구 코드는 기존 비공식 번호 그대로다`() {
        val codes = regionFacade.listRegions(ListRegionsCommand(SEOUL)).map { it.code }
        assertEquals("KR-11-110", codes.first())
        assertEquals("KR-11-830", codes.last())
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
        private const val SEJONG = "KR-36"
        private const val PROVINCE_COUNT = 16
        private val EXPECTED_CHILD_COUNTS = mapOf(
            SEOUL to SEOUL_DISTRICT_COUNT,
            "KR-12" to 27,
            "KR-27" to 9,
            "KR-28" to 11,
            "KR-41" to 47,
            "KR-50" to 2,
            SEJONG to 0,
        )
    }
}
