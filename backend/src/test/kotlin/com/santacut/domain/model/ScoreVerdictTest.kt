package com.santacut.domain.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ScoreVerdictTest {
    @Test
    fun `구간 하한 점수부터 그 구간 판정을 받는다`() {
        assertEquals(ScoreVerdict.SANTA_FAMILY, ScoreVerdict.of(10000))
        assertEquals(ScoreVerdict.SANTA_FAMILY, ScoreVerdict.of(9000))
        assertEquals(ScoreVerdict.GIFT_DEPARTED, ScoreVerdict.of(8999))
        assertEquals(ScoreVerdict.GIFT_DEPARTED, ScoreVerdict.of(7500))
        assertEquals(ScoreVerdict.DELIVERY_ON_HOLD, ScoreVerdict.of(7499))
        assertEquals(ScoreVerdict.DELIVERY_ON_HOLD, ScoreVerdict.of(5000))
        assertEquals(ScoreVerdict.COAL_ON_DELIVERY, ScoreVerdict.of(4999))
        assertEquals(ScoreVerdict.COAL_ON_DELIVERY, ScoreVerdict.of(0))
    }

    @Test
    fun `판정 문구와 한마디는 기존 문구를 그대로 쓴다`() {
        assertEquals("산타 친자 의혹", ScoreVerdict.SANTA_FAMILY.title)
        assertEquals("이 정도면 선물 말고 상속을 기다려라.", ScoreVerdict.SANTA_FAMILY.comment)
        assertEquals("선물 배송 출발", ScoreVerdict.GIFT_DEPARTED.title)
        assertEquals("굴뚝 앞 배송 보류", ScoreVerdict.DELIVERY_ON_HOLD.title)
        assertEquals("석탄 착불 배송", ScoreVerdict.COAL_ON_DELIVERY.title)
        assertEquals("크리스마스 선물인데 새해 복 많이 받아라.", ScoreVerdict.COAL_ON_DELIVERY.comment)
    }
}
