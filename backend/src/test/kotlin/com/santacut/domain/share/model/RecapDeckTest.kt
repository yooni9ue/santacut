package com.santacut.domain.share.model

import com.santacut.TestDossiers
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecapDeckTest {
    private val mapper = TestDossiers.mapper

    @Test
    fun `카드 덱은 2판을 붙인 JSON으로 저장하고 판을 확인한 트리에서 같은 값으로 다시 읽는다`() {
        val json = mapper.writeValueAsString(DECK)

        assertTrue(json.startsWith("{\"version\":2,"))
        assertTrue(json.contains("\"bands\":[{"))
        assertEquals(DECK, RecapDeck.from(mapper.readTree(json), mapper))
    }

    @Test
    fun `카드 종류가 쓰지 않는 필드와 농담을 끈 문구는 저장하지 않는다`() {
        val json = mapper.writeValueAsString(DECK)

        assertFalse(json.contains("null"))
        assertFalse(json.contains("Mild", ignoreCase = true))
        assertFalse(json.contains("\"gift\""))
    }

    @Test
    fun `모르는 종류의 카드는 덱을 버리지 않고 그 카드만 건너뛴다`() {
        val json = mapper.writeValueAsString(DECK).replaceFirst("\"kind\":\"CUTLINE\"", "\"kind\":\"FUTURE_KIND\"")

        val read = RecapDeck.from(mapper.readTree(json), mapper)

        assertEquals(DECK.cards.size() - 1, read.cards.size())
    }

    @Test
    fun `도착 예정일은 남이 보는 화면이라 서울 날짜만 표시한다`() {
        assertEquals("2026년 12월 25일", VERDICT.displayDeliveryEta())
        assertEquals(null, VERDICT.copy(deliveryEta = null).displayDeliveryEta())
    }

    companion object {
        private val VERDICT = RecapDeckCard(
            0, RecapDeckKind.VERDICT, "73.73", 3, 50, verdict = "착한 아이", passed = true, grade = "굴뚝에 걸쳐서 통과",
            cutDistance = "+8.73", verdictLine = "판정 한 줄.", deliveryEta = "2026-12-24T15:30:00Z",
        )
        private val DECK = RecapDeck.of(
            "GIFT",
            RecapDeckCards(
                listOf(
                    VERDICT,
                    RecapDeckCard(
                        1, RecapDeckKind.CUTLINE, "73.73", 3, 50, grade = "굴뚝에 걸쳐서 통과", bandDetail = "바지 걸렸는데 통과",
                        bands = RecapDeckBands(listOf(RecapDeckBand("굴뚝에 걸쳐서 통과", "65~74", true))),
                    ),
                    RecapDeckCard(
                        2, RecapDeckKind.REASON, "73.73", 3, 50, ordinal = 1, quote = "내일 몇 시에 볼까", chipText = "'ㄱ?' 발사 횟수 19번",
                        label = "약속발사상", line = "판은 늘 여기서 열렸다.", dimTag = "협력 +",
                    ),
                ),
            ),
        )
    }
}
