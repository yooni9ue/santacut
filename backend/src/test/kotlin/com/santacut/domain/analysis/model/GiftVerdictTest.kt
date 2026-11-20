package com.santacut.domain.analysis.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GiftVerdictTest {
    @Test
    fun `65점부터 선물을 받고 그 아래는 석탄이다`() {
        assertTrue(GiftVerdict(6500).passed)
        assertFalse(GiftVerdict(6499).passed)
        assertEquals("선물 배송 확정", GiftVerdict(6500).stamp())
        assertEquals("석탄", GiftVerdict(6499).stamp())
        assertEquals("선물은 내년에", GiftVerdict(6499).stampMild())
        assertEquals("선물 배송 확정", GiftVerdict(6500).stampMild())
    }

    @Test
    fun `등급은 구간 하한부터 그 등급이다`() {
        assertEquals(GiftGrade.PREMIUM, GiftGrade.of(9000))
        assertEquals(GiftGrade.GIFT_DEPARTED, GiftGrade.of(8999))
        assertEquals(GiftGrade.GIFT_DEPARTED, GiftGrade.of(7500))
        assertEquals(GiftGrade.SCRAPED_THROUGH, GiftGrade.of(7499))
        assertEquals(GiftGrade.SCRAPED_THROUGH, GiftGrade.of(6500))
        assertEquals(GiftGrade.ONE_STEP_SHORT, GiftGrade.of(6499))
        assertEquals(GiftGrade.ONE_STEP_SHORT, GiftGrade.of(5500))
        assertEquals(GiftGrade.OFF_LIST, GiftGrade.of(5499))
        assertEquals(GiftGrade.OFF_LIST, GiftGrade.of(5000))
        assertEquals(GiftGrade.COAL, GiftGrade.of(4999))
        assertEquals(GiftGrade.COAL, GiftGrade.of(0))
    }

    @Test
    fun `컷 거리는 부호를 늘 붙이고 소수 둘째 자리까지 쓴다`() {
        assertEquals("+8.73", GiftVerdict(7373).cutDistanceText())
        assertEquals(873, GiftVerdict(7373).cutDistanceHundredths())
        assertEquals("-3.20", GiftVerdict(6180).cutDistanceText())
        assertEquals("+0.00", GiftVerdict(6500).cutDistanceText())
        assertEquals("-0.01", GiftVerdict(6499).cutDistanceText())
        assertEquals("73.73", GiftVerdict(7373).scoreText())
        assertEquals("5.07", GiftVerdict(507).scoreText())
    }

    @Test
    fun `판결 제목은 도장과 등급을 잇고 석탄 등급은 도장만 쓴다`() {
        assertEquals("선물 배송 확정 · 턱걸이 통과", GiftVerdict(7373).headline())
        assertEquals("석탄 · 한 끗", GiftVerdict(6000).headline())
        assertEquals("석탄", GiftVerdict(3000).headline())
        assertEquals("선물은 내년에 · 명단 밖", GiftVerdict(5200).mildHeadline())
        assertEquals("선물은 내년에", GiftVerdict(3000).mildHeadline())
    }

    @Test
    fun `컷과 3점 안이면 아슬아슬, 위로 넉넉, 아래로 한참이다`() {
        assertEquals(CutMargin.CLOSE, GiftVerdict(6799).margin())
        assertEquals(CutMargin.AMPLE, GiftVerdict(6800).margin())
        assertEquals(CutMargin.CLOSE, GiftVerdict(6201).margin())
        assertEquals(CutMargin.FAR, GiftVerdict(6200).margin())
    }

    @Test
    fun `스킨과 공유 문구는 판정을 따른다`() {
        assertEquals("GIFT", GiftVerdict(7373).skin())
        assertEquals("COAL", GiftVerdict(4000).skin())
        assertEquals("산타 공식 인증. 올해 선물 받음. 너는?", GiftVerdict(7373).shareText())
        assertEquals("나 올해 석탄이래. 사유 봐라.", GiftVerdict(4000).shareText())
    }
}
