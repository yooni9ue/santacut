package com.santacut.domain.participant.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RegionsTest {
    private val seoul = Region("KR-11", "서울특별시", 37.5665, 126.9780)
    private val jongno = Region("KR-11-110", "종로구", 37.5729, 126.9794, "KR-11")
    private val junggu = Region("KR-11-140", "중구", 37.5637, 126.9975, "KR-11")
    private val sejong = Region("KR-50", "세종특별자치시", 36.4800, 127.2890)
    private val regions = Regions(listOf(seoul, jongno, junggu, sejong))

    @Test
    fun `지역 코드로 지역을 찾는다`() {
        assertSame(junggu, regions.find("KR-11-140"))
        assertNull(regions.find("KR-00-000"))
    }

    @Test
    fun `상위 지역이 없으면 최상위 지역을, 있으면 그 바로 아래 지역을 고른다`() {
        assertEquals(listOf("KR-11", "KR-50"), regions.childrenOf(null).map { it.code })
        assertEquals(listOf("KR-11-110", "KR-11-140"), regions.childrenOf("KR-11").map { it.code })
        assertTrue(regions.childrenOf("KR-99").map { it.code }.isEmpty())
    }

    @Test
    fun `하위 지역이 있는 시·도는 고를 수 없고 구와 구가 없는 시·도는 고를 수 있다`() {
        assertFalse(regions.isSelectable(seoul))
        assertTrue(regions.isSelectable(jongno))
        assertTrue(regions.isSelectable(sejong))
        assertFalse(regions.isSelectable("KR-11"))
        assertTrue(regions.isSelectable("KR-50"))
        assertFalse(regions.isSelectable("KR-00-000"))
    }

    @Test
    fun `목록 순서대로 각 지역을 바꾼다`() {
        assertEquals(listOf("종로구", "중구"), Regions(listOf(jongno, junggu)).map { it.name })
    }

    @Test
    fun `빈 지역 목록은 아무것도 찾지 못하고 바꿀 것도 없다`() {
        val empty = Regions(emptyList())
        assertNull(empty.find("KR-11-110"))
        assertFalse(empty.isSelectable("KR-11-110"))
        assertTrue(empty.map { it.code }.isEmpty())
    }
}
