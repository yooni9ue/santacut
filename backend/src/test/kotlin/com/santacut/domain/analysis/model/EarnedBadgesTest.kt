package com.santacut.domain.analysis.model

import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals

class EarnedBadgesTest {
    @Test
    fun `본인 말에서만 낱말을 세어 배지를 준다`() {
        val turns = TranscriptTurns(
            List(5) { turn(ScoringMessage.SELF, "고마워 진짜") } + List(10) { turn(PARTNER, "미안 고마워") },
        )

        val badges = EarnedBadges.countedIn(turns)

        assertEquals(listOf("고마워 5회 · 감사 자판기", "미안 0회 · 사과 품절"), badges.labels())
        assertEquals("고마워 5회 · 감사 자판기", badges.firstLabel())
    }

    @Test
    fun `평가 행 값으로 저장하고 다시 읽는다`() {
        val badges = EarnedBadges(listOf(EarnedBadge(BadgeRule.THANKS_MACHINE, 12), EarnedBadge(BadgeRule.APOLOGY_SOLD_OUT, 0)))

        assertEquals("THANKS_MACHINE:12,APOLOGY_SOLD_OUT:0", badges.toColumn())
        assertEquals(badges.labels(), EarnedBadges.fromColumn(badges.toColumn()).labels())
        assertEquals(emptyList(), EarnedBadges.fromColumn(null).labels())
        assertEquals(emptyList(), EarnedBadges.fromColumn("UNKNOWN:3").labels())
    }

    private fun turn(speaker: String, text: String): TranscriptTurn = TranscriptTurn(speaker, NOON, NOON, text)

    companion object {
        private const val PARTNER = "상대"
        private val NOON: LocalDateTime = LocalDateTime.of(2026, 11, 1, 12, 0)
    }
}
