package com.santacut.domain.participant.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RegionsTest {
    private val jongno = Region("KR-11-110", "종로구", 37.5729, 126.9794)
    private val junggu = Region("KR-11-140", "중구", 37.5637, 126.9975)

    @Test
    fun `지역 코드로 지역을 찾는다`() {
        val regions = Regions(listOf(jongno, junggu))
        assertSame(junggu, regions.find("KR-11-140"))
        assertNull(regions.find("KR-00-000"))
    }

    @Test
    fun `목록 순서대로 각 지역을 바꾼다`() {
        assertEquals(listOf("종로구", "중구"), Regions(listOf(jongno, junggu)).map { it.name })
    }

    @Test
    fun `빈 지역 목록은 아무것도 찾지 못하고 바꿀 것도 없다`() {
        val regions = Regions(emptyList())
        assertNull(regions.find("KR-11-110"))
        assertTrue(regions.map { it.code }.isEmpty())
    }
}
