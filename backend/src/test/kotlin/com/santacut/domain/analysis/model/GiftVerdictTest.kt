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
    }

    @Test
    fun `도장은 등급마다 다르고 농담을 끈 도장은 탈락 세 등급에서 내년 명단 예약이다`() {
        assertEquals(
            listOf("산타 최애", "선물 실었다", "겨우 실었다", "아깝다 진짜", "산타 읽씹", "반품 불가"),
            listOf(9000, 7500, 6500, 6499, 5000, 4999).map { hundredths -> GiftVerdict(hundredths).stamp() },
        )
        assertEquals(
            listOf("산타 최애", "선물 실었다", "겨우 실었다", "내년 명단 예약", "내년 명단 예약", "내년 명단 예약"),
            listOf(9000, 7500, 6500, 6499, 5000, 4999).map { hundredths -> GiftVerdict(hundredths).stampMild() },
        )
    }

    @Test
    fun `등급 이름은 굴뚝 프리패스부터 석탄 당첨까지다`() {
        assertEquals(
            listOf("굴뚝 프리패스", "썰매 탑승 완료", "굴뚝에 걸쳐서 통과", "굴뚝 앞 유턴", "명단에 없음", "석탄 당첨"),
            GiftGrade.entries.map(GiftGrade::label),
        )
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
    fun `판결 제목은 도장과 등급을 잇고 농담을 끈 제목은 석탄 등급 이름을 쓰지 않는다`() {
        assertEquals("겨우 실었다 · 굴뚝에 걸쳐서 통과", GiftVerdict(7373).headline())
        assertEquals("아깝다 진짜 · 굴뚝 앞 유턴", GiftVerdict(6000).headline())
        assertEquals("반품 불가 · 석탄 당첨", GiftVerdict(3000).headline())
        assertEquals("내년 명단 예약 · 명단에 없음", GiftVerdict(5200).mildHeadline())
        assertEquals("내년 명단 예약", GiftVerdict(3000).mildHeadline())
    }

    @Test
    fun `컷 거리 말은 등급 경계를 따라 컷 위아래 10점 안이 아슬아슬이다`() {
        assertEquals(CutMargin.AMPLE, GiftVerdict(7500).margin())
        assertEquals(CutMargin.CLOSE, GiftVerdict(7499).margin())
        assertEquals(CutMargin.CLOSE, GiftVerdict(7055).margin())
        assertEquals(CutMargin.CLOSE, GiftVerdict(5500).margin())
        assertEquals(CutMargin.FAR, GiftVerdict(5499).margin())
    }

    @Test
    fun `작가에게 보내는 등급 온도는 굴뚝에 걸쳐서 통과면 아슬아슬 통과다`() {
        assertEquals(
            listOf("넉넉한 통과", "넉넉한 통과", "아슬아슬 통과", "아깝게 탈락", "석탄", "석탄"),
            GiftGrade.entries.map(GiftGrade::temperature),
        )
    }

    @Test
    fun `스킨은 판정을, 공유 문구는 등급을 따르고 총점 표시를 넣는다`() {
        assertEquals("GIFT", GiftVerdict(7373).skin())
        assertEquals("COAL", GiftVerdict(4000).skin())
        assertEquals("산타가 내 단톡방 다 읽고도 92.10점 줌. 이게 인성이다. 너도 까봐.", GiftVerdict(9210).shareText())
        assertEquals("산타가 내 단톡방 다 읽음. 80.00점, 선물 실림. 넌 읽히면 석탄 각.", GiftVerdict(8000).shareText())
        assertEquals("산타가 내 단톡방 다 읽음. 70.55점, 굴뚝에 걸쳐서 겨우 통과. 넌 넘냐?", GiftVerdict(7055).shareText())
        assertEquals("60.00점. 굴뚝 앞에서 썰매가 유턴함. 억울해서 올린다. 너는 넘나 보자.", GiftVerdict(6000).shareText())
        assertEquals("산타한테 읽씹당함. 52.00점. 나만 당할 수 없다. 너도 까봐.", GiftVerdict(5200).shareText())
        assertEquals("산타가 내 단톡방 읽고 석탄 보냄. 반품도 안 된대. 혼자 받긴 억울하다. 너도 까봐.", GiftVerdict(4000).shareText())
    }

    @Test
    fun `농담을 끈 공유 문구는 통과면 공유 문구와 같고 탈락이면 점수 없는 문구 하나다`() {
        assertEquals(GiftVerdict(9210).shareText(), GiftVerdict(9210).shareTextMild())
        assertEquals("올해 선물은 내년으로 미뤄짐. 너는 받았는지 까봐.", GiftVerdict(6499).shareTextMild())
        assertEquals("올해 선물은 내년으로 미뤄짐. 너는 받았는지 까봐.", GiftVerdict(4000).shareTextMild())
    }
}
