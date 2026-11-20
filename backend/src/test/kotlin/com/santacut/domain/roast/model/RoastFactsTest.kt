package com.santacut.domain.roast.model

import com.santacut.TestRoastStats
import com.santacut.TestRoasts
import com.santacut.domain.dossier.model.DossierStats
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 시드로 고르는 작가 사실. 같은 시드면 같고, 시드가 다르면 섞이며, 맨 앞은 늘 가장 센 사실이고 계열은 겹치지 않는다. */
class RoastFactsTest {
    private val heavy = TestRoastStats.heavyUser

    @Test
    fun `같은 시드면 같은 사실이 같은 순서로 나온다`() {
        assertEquals(factsOf(heavy, 42).keys(), factsOf(heavy, 42).keys())
    }

    @Test
    fun `시드가 다르면 고른 사실이 달라진다`() {
        val decks = SEEDS.map { seed -> factsOf(heavy, seed).keys().take(SHOWN) }.toSet()

        assertTrue(decks.size >= MIN_DISTINCT_DECKS, "서로 다른 앞 세 장 조합 ${decks.size}개")
    }

    @Test
    fun `맨 앞은 어느 시드에서나 점수가 가장 큰 사실이다`() {
        SEEDS.forEach { seed ->
            val facts = factsOf(heavy, seed).toList()
            assertEquals(facts.maxOf(RoastFact::score), facts.first().score, "시드 $seed")
        }
        assertEquals(setOf("question_ratio"), SEEDS.map { seed -> factsOf(heavy, seed).keys().first() }.toSet())
    }

    @Test
    fun `점수가 같은 맨 앞 후보는 시드로 돌아가며 나온다`() {
        val tied = heavy.copy(emoji = heavy.emoji.copy(density = 30.0))

        val firsts = SEEDS.map { seed -> factsOf(tied, seed).keys().first() }.toSet()

        assertTrue(firsts.size > 1, "맨 앞 후보 $firsts")
    }

    @Test
    fun `뽑은 사실은 계열이 겹치지 않고 앞의 세 사실은 계열이 모두 다르다`() {
        SEEDS.forEach { seed ->
            val facts = factsOf(heavy, seed).toList()
            val drawn = facts.take(RoastFacts.MAX_DRAWN_FACTS).filterNot { fact -> fact.group == HABIT && fact != firstHabit(facts) }
            assertEquals(drawn.size, drawn.map(RoastFact::group).toSet().size, "시드 $seed")
            assertEquals(SHOWN, facts.take(SHOWN).map(RoastFact::group).toSet().size, "시드 $seed")
            assertTrue(facts.size in MIN_FACTS..RoastFacts.MAX_DRAWN_FACTS + 1, "시드 $seed 사실 ${facts.size}개")
        }
    }

    @Test
    fun `메시지 양에 따라 늘 튀는 지표가 고인물의 앞자리를 독차지하지 않는다`() {
        val shown = SEEDS.flatMap { seed -> factsOf(heavy, seed).keys().take(SHOWN) }

        VOLUME_TIED.forEach { key ->
            val share = shown.count { shownKey -> shownKey == key }.toDouble() / SEEDS.count()
            assertTrue(share <= MAX_VOLUME_SHARE, "$key 앞 세 장 비율 $share")
        }
    }

    @Test
    fun `시드 이백 개에서 맨 앞 다음 두 자리는 한 사실이 육십 퍼센트를 넘지 않는다`() {
        val decks = SEEDS.map { seed -> factsOf(heavy, seed).keys().drop(1).take(SHOWN - 1) }

        val shares = decks.flatten().groupingBy { key -> key }.eachCount().mapValues { (_, count) -> count.toDouble() / decks.size }

        assertTrue(shares.values.all { share -> share <= MAX_SHARE }, shares.toString())
    }

    private fun factsOf(stats: DossierStats, seed: Int): RoastFacts =
        RoastFacts.of(stats.refreshedBy(TestRoasts.dossier.phrases), TestRoasts.dossier, TestRoasts.catalog, seed)

    private fun firstHabit(facts: List<RoastFact>): RoastFact? = facts.firstOrNull { fact -> fact.group == HABIT }

    companion object {
        private val SEEDS = 1..200
        private const val SHOWN = 3
        private const val HABIT = "habit"
        private const val MIN_FACTS = 8
        private const val MIN_DISTINCT_DECKS = 20
        private const val MAX_SHARE = 0.6
        private const val MAX_VOLUME_SHARE = 0.5
        private val VOLUME_TIED = listOf("message_count", "active_days", "burst_max", "longest_message_length", "laugh_max_run")
    }
}
