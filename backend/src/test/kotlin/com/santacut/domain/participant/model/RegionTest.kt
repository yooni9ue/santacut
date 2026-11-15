package com.santacut.domain.participant.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RegionTest {
    private val seoul = Region("KR-11", "서울특별시", 37.5665, 126.9780)

    @Test
    fun `코드에 구분자를 붙인 것이 접두어인 지역의 상위 지역이다`() {
        assertTrue(seoul.isParentOf(Region("KR-11-110", "종로구", 37.5729, 126.9794)))
        assertFalse(seoul.isParentOf(Region("KR-110", "다른 지역", 0.0, 0.0)))
        assertFalse(seoul.isParentOf(seoul))
    }

    @Test
    fun `상위 지역을 이으면 그 코드를 가지고 이어진 상태가 된다`() {
        val jongno = Region("KR-11-110", "종로구", 37.5729, 126.9794)
        assertFalse(jongno.isLinked())

        assertSame(jongno, jongno.linkTo(seoul))

        assertTrue(jongno.isLinked())
        assertEquals("KR-11", jongno.parentCode)
    }
}
