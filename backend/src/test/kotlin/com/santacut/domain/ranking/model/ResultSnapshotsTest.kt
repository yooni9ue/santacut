package com.santacut.domain.ranking.model

import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ResultSnapshotsTest {
    private val eta = Instant.parse("2026-12-25T00:03:00Z")
    private val computedAt = Instant.parse("2026-12-01T00:00:00Z")

    @Test
    fun `주어진 ID의 스냅숏을 빼고 나머지를 순서대로 남긴다`() {
        val snapshots = ResultSnapshots(listOf(snapshot("a"), snapshot("b"), snapshot("c")))
        assertEquals(listOf("a", "c"), snapshots.excluding(setOf("b", "missing")).toList().map { it.id })
    }

    @Test
    fun `모두 빼면 빈 목록이고 아무것도 빼지 않으면 그대로다`() {
        val snapshots = ResultSnapshots(listOf(snapshot("a"), snapshot("b")))
        assertTrue(snapshots.excluding(setOf("a", "b")).isEmpty())
        assertFalse(snapshots.isEmpty())
        assertEquals(listOf("a", "b"), snapshots.excluding(emptySet()).toList().map { it.id })
    }

    @Test
    fun `밖으로 넘긴 목록을 바꿔도 원래 목록은 그대로다`() {
        val snapshots = ResultSnapshots(listOf(snapshot("a")))
        val copy = snapshots.toList().toMutableList()
        copy.clear()
        assertEquals(listOf("a"), snapshots.toList().map { it.id })
    }

    private fun snapshot(id: String): ResultSnapshot =
        ResultSnapshot(id, "user-$id", "assessment-$id", 1, 1, eta, computedAt)
}
