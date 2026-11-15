package com.santacut.domain.share.model

import com.santacut.domain.ranking.model.ResultSnapshot
import com.santacut.domain.ranking.model.ResultSnapshots
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals

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
    fun `도착 예정일은 서울 시각으로 표시한다`() {
        assertEquals("2026년 12월 25일 09:03", share.displayEta())
    }

    @Test
    fun `계산 시각은 데이터베이스 UTC 형식으로 표시한다`() {
        assertEquals("2026-12-01 01:02:03.004", share.displayComputedAt())
    }

    @Test
    fun `공유 링크 주소는 공개 출처 아래 페이지와 이미지 경로다`() {
        val link = ShareLink.of("https://santacut.example", share)
        assertEquals("token", link.token)
        assertEquals("https://santacut.example/s/token", link.url)
        assertEquals("https://santacut.example/s/token/image.png", link.imageUrl)
    }

    @Test
    fun `공유 링크가 붙잡은 스냅숏은 정리 대상에서 빠진다`() {
        val kept = snapshot("snapshot")
        val stale = snapshot("stale")
        val sharedIds = Shares(listOf(share)).snapshotIds()
        assertEquals(setOf("snapshot"), sharedIds)
        assertEquals(listOf(stale), ResultSnapshots(listOf(kept, stale)).excluding(sharedIds).toList())
    }

    private fun snapshot(id: String): ResultSnapshot =
        ResultSnapshot(id, "user", "assessment", 1, 1, share.eta, share.computedAt)
}
