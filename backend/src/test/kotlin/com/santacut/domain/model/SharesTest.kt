package com.santacut.domain.model

import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SharesTest {
    private val eta = Instant.parse("2026-12-25T00:03:00Z")
    private val computedAt = Instant.parse("2026-12-01T00:00:00Z")

    @Test
    fun `공유 링크가 붙잡은 스냅숏 ID를 중복 없이 모은다`() {
        val shares = Shares(listOf(share("t1", "snapshot-1"), share("t2", "snapshot-1"), share("t3", "snapshot-2")))
        assertEquals(setOf("snapshot-1", "snapshot-2"), shares.snapshotIds())
    }

    @Test
    fun `공유 링크가 없으면 붙잡은 스냅숏도 없다`() {
        assertTrue(Shares(emptyList()).snapshotIds().isEmpty())
    }

    private fun share(token: String, snapshotId: String): Share =
        Share(token, "user", snapshotId, "산타", 8000, 1, eta, "선물 배송 출발", "한마디", computedAt)
}
