package com.santacut.domain.analysis.model

import org.junit.jupiter.api.Test
import java.time.YearMonth
import kotlin.test.assertEquals

/**
 * 예산을 달마다 고르게 나눠 고르는 규칙을 확인한다.
 */
class SpreadCandidatesTest {
    @Test
    fun `몫 합이 예산 이하면 모두 그대로 남긴다`() {
        val candidates = candidates(JANUARY to 3, MARCH to 2)

        assertEquals(listOf("1-0", "1-1", "1-2", "3-0", "3-1"), candidates.within(5))
    }

    @Test
    fun `최근 달에 대화가 몰려도 예전 달에 몫을 똑같이 나눈다`() {
        val candidates = candidates(JANUARY to 10, MARCH to 10, NOVEMBER to 100)

        assertEquals(listOf("1-7", "1-8", "1-9", "3-7", "3-8", "3-9", "11-97", "11-98", "11-99"), candidates.within(9))
    }

    @Test
    fun `대화가 적은 달이 다 못 쓴 예산은 대화가 많은 달에 넘긴다`() {
        val kept = candidates(JANUARY to 1, MARCH to 2, NOVEMBER to 100).within(12)

        assertEquals(listOf("1-0", "3-0", "3-1"), kept.take(3))
        assertEquals((91..99).map { "11-$it" }, kept.drop(3))
    }

    @Test
    fun `몫이 0인 후보는 예산과 상관없이 늘 남긴다`() {
        val month = YearMonth.of(2026, NOVEMBER)
        val candidates = SpreadCandidates(
            listOf(
                SpreadCandidate("상대-옛날", month, 0),
                SpreadCandidate("나-옛날", month, 1),
                SpreadCandidate("상대-최근", month, 0),
                SpreadCandidate("나-최근", month, 1),
            ),
        )

        assertEquals(listOf("상대-옛날", "상대-최근", "나-최근"), candidates.within(1))
    }

    /** 달마다 [counts]개씩 몫 1인 후보를 `달-순번` 이름으로 시각순으로 만든다. */
    private fun candidates(vararg counts: Pair<Int, Int>): SpreadCandidates<String> = SpreadCandidates(
        counts.flatMap { (month, count) -> List(count) { index -> SpreadCandidate("$month-$index", YearMonth.of(2026, month), 1) } },
    )

    companion object {
        private const val JANUARY = 1
        private const val MARCH = 3
        private const val NOVEMBER = 11
    }
}
