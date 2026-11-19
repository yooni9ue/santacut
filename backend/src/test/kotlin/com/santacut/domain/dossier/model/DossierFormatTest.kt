package com.santacut.domain.dossier.model

import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals

class DossierFormatTest {
    @Test
    fun `횟수와 순위는 천 단위 쉼표와 단위를 붙인다`() {
        assertEquals("1,234번", DossierFormat.times(1234))
        assertEquals("전국 3,412위", DossierFormat.nationalRank(3412))
        assertEquals("212개", DossierFormat.amount(211.6, "개"))
    }

    @Test
    fun `소수는 첫째 자리까지 쓰고 끝의 0은 뗀다`() {
        assertEquals("3.8개", DossierFormat.decimal(3.84, "개"))
        assertEquals("7배", DossierFormat.decimal(7.0, "배"))
        assertEquals("1,234.5자", DossierFormat.decimal(1234.5, "자"))
        assertEquals("23%", DossierFormat.percent(23.4))
    }

    @Test
    fun `분은 10분 단위 시간대로 뭉갠다`() {
        assertEquals("7분", DossierFormat.minutes(7))
        assertEquals("40분대", DossierFormat.minutes(47))
        assertEquals("2시간대", DossierFormat.minutes(125))
        assertEquals("2시간 40분대", DossierFormat.minutes(161))
    }

    @Test
    fun `시는 시간대 이름으로 바꾼다`() {
        assertEquals(
            listOf("밤 12시대", "새벽 1시대", "오전 9시대", "낮 12시대", "오후 3시대", "저녁 6시대", "저녁 8시대", "밤 9시대", "밤 11시대"),
            listOf(0, 1, 9, 12, 15, 18, 20, 21, 23).map(DossierFormat::hour),
        )
    }

    @Test
    fun `티저 숫자는 가리고 도착 시각은 한국 시각으로 쓴다`() {
        assertEquals("██,███개", DossierFormat.masked("12,345개"))
        assertEquals("12월 25일 오전 6시 12분", DossierFormat.eta(Instant.parse("2026-12-24T21:12:00Z")))
    }
}
