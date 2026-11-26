package com.santacut.domain.share.model

import com.santacut.TestDossiers
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecapDeckTest {
    private val mapper = TestDossiers.mapper

    @Test
    fun `카드 덱은 판을 붙인 JSON으로 저장했다가 같은 값으로 다시 읽는다`() {
        val json = mapper.writeValueAsString(DECK)

        assertEquals(DECK, mapper.readValue(json, RecapDeck::class.java))
        assertTrue(json.startsWith("{\"version\":1,"))
        assertTrue(json.contains("\"cards\":[{"))
        assertTrue(json.contains("\"acts\":[{\"act\":1"))
    }

    @Test
    fun `카드 종류가 쓰지 않는 필드와 농담을 끈 문구는 저장하지 않는다`() {
        val json = mapper.writeValueAsString(DECK)

        assertFalse(json.contains("null"))
        assertFalse(json.contains("Mild", ignoreCase = true))
    }

    @Test
    fun `지금 코드가 모르는 판은 현재 판이 아니다`() {
        assertTrue(DECK.hasCurrentVersion())
        assertFalse(DECK.copy(version = RecapDeck.VERSION + 1).hasCurrentVersion())
    }

    @Test
    fun `도착 예정일은 남이 보는 화면이라 서울 날짜만 표시한다`() {
        assertEquals("2026년 12월 25일", VERDICT.displayDeliveryEta())
        assertEquals(null, VERDICT.copy(deliveryEta = null).displayDeliveryEta())
    }

    companion object {
        private val VERDICT = RecapDeckCard(
            0, RecapDeckKind.VERDICT, "73.73", title = "칭호", verdict = "판결", stamp = "겨우 실었다", grade = "굴뚝에 걸쳐서 통과",
            gradeCode = "SCRAPED_THROUGH", cut = 65, cutDistance = "+8.73", passed = true, rank = 3, rankingPending = false,
            deliveryEta = "2026-12-24T15:30:00Z",
        )
        private val DECK = RecapDeck.of(
            "GIFT",
            RecapDeckCards(
                listOf(
                    VERDICT,
                    RecapDeckCard(1, RecapDeckKind.ROAST, "73.73", chipLabel = "ㅋ 최대", chipValue = "12개", roast = "놀림", flip = "근데 뒤집기"),
                    RecapDeckCard(2, RecapDeckKind.STORY, "73.73", acts = RecapDeckActs(listOf(RecapDeckAct(1, "1~4월", "첫 막")))),
                    RecapDeckCard(3, RecapDeckKind.DIMENSIONS, "73.73", bars = RecapDeckBars(listOf(RecapDeckBar("공감", BigDecimal("71.67"))))),
                ),
            ),
        )
    }
}
