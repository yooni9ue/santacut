package com.santacut.domain.share.model

import com.santacut.domain.ranking.model.RankingStage
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShareTest {
    private val share = Share(
        "token", "user", "snapshot", "산타도와줘", 8245, 3,
        Instant.parse("2026-12-25T00:03:00Z"), "선물 배송 출발", "한마디",
        Instant.parse("2026-12-01T01:02:03.004Z"),
    )

    @Test
    fun `점수는 소수점 둘째 자리까지 표시한다`() {
        assertEquals("82.45", share.displayScore())
        val lowScore = Share("t", "u", "s", "n", 500, 1, share.eta, "v", "c", share.computedAt)
        assertEquals("5.00", lowScore.displayScore())
    }

    @Test
    fun `도착 예정일은 남이 보는 화면이라 서울 날짜만 표시한다`() {
        assertEquals("2026년 12월 25일", share.displayEta())
    }

    @Test
    fun `계산 시각은 서울 시각으로 표시한다`() {
        assertEquals("2026년 12월 1일 10:02", share.displayComputedAt())
    }

    @Test
    fun `공동 순위면 공동을 붙이고 상태 이름을 한국어로 표시한다`() {
        assertEquals("3위", share.displayRank())
        assertEquals("미리보기", share.displayStage())
        val tiedFinal = Share(
            "t", "u", "s", "n", 500, 2, share.eta, "v", "c", share.computedAt, true, RankingStage.FINAL, SHARED_AT,
        )
        assertEquals("공동 2위", tiedFinal.displayRank())
        assertEquals("시즌 확정", tiedFinal.displayStage())
    }

    @Test
    fun `공유 시각은 서울 시각으로 표시하고 기존 행처럼 비어 있으면 계산 시각을 쓴다`() {
        assertEquals(share.computedAt, share.sharedAt())
        val shared = Share("t", "u", "s", "n", 500, 2, share.eta, "v", "c", share.computedAt, createdAt = SHARED_AT)
        assertEquals(SHARED_AT, shared.sharedAt())
        assertEquals("2026년 12월 2일 12:30", shared.displaySharedAt())
    }

    @Test
    fun `공유 링크 주소는 공개 출처 아래 페이지와 이미지 경로다`() {
        val link = ShareLink.of("https://santacut.example", share)
        assertEquals("token", link.token)
        assertEquals("https://santacut.example/s/token", link.url)
        assertEquals("https://santacut.example/s/token/image.png", link.imageUrl)
    }

    @Test
    fun `한마디는 고를 때만 싣고 다시 빼거나 실을 수 있다`() {
        val quoted = Share("t", "u", "s", "n", 500, 1, share.eta, "v", "c", share.computedAt)

        quoted.chooseQuote("본인 한마디", includeQuote = true)
        assertTrue(quoted.carriesQuote())
        assertEquals("본인 한마디", quoted.quote)
        quoted.chooseQuote("본인 한마디", includeQuote = false)
        assertFalse(quoted.carriesQuote())
        assertNull(quoted.quote)
    }

    @Test
    fun `리캡 카드 덱은 없는 링크에만 한 번 싣고 이미 있으면 그대로 둔다`() {
        val legacy = Share("t", "u", "s", "n", 500, 1, share.eta, "v", "c", share.computedAt)
        assertFalse(legacy.carriesRecap())

        legacy.attachRecap(FIRST_DECK)
        legacy.attachRecap(SECOND_DECK)

        assertTrue(legacy.carriesRecap())
        assertEquals(FIRST_DECK, legacy.recapJson)
    }

    companion object {
        private const val FIRST_DECK = "{\"version\":1,\"cards\":[]}"
        private const val SECOND_DECK = "{\"version\":2,\"cards\":[]}"
        private val SHARED_AT: Instant = Instant.parse("2026-12-02T03:30:00Z")
    }
}
