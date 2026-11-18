package com.santacut.domain.analysis.model

import java.time.YearMonth

/**
 * 시각순 후보 목록. 예산을 넘으면 달마다 고르게 나눠 고른다. 최신 대화만 남기면 대화가 많은 사람은 최근 몇 주만 남아
 * 올해 대화 전체를 보지 못하기 때문이다.
 */
class SpreadCandidates<T>(private val values: List<SpreadCandidate<T>>) {
    /**
     * 몫의 합이 [budget] 이하가 되게 고른 값 목록. 시각순 그대로다.
     * 예산은 후보가 있는 달에 똑같이 나누고, 몫이 그보다 적은 달이 남긴 예산은 나머지 달에 다시 나눈다.
     * 달마다 최신 후보부터 그 달 예산까지 이어진 구간을 남겨 문맥이 끊기지 않게 한다. 몫이 0인 후보는 늘 남긴다.
     * 한 달 안에서 다음 후보가 그 달 예산을 넘겨 남은 자투리는 다른 달로 넘기지 않는다. 그래서 몫 합이 예산보다 조금 모자랄 수 있다.
     */
    fun within(budget: Int): List<T> {
        if (values.sumOf { it.cost } <= budget) return values.map { it.value }
        val shares = monthShares(budget)
        return values.groupBy { it.month }.toSortedMap().entries
            .flatMap { (month, candidates) -> newestWithin(candidates, shares.getValue(month)) }
            .map { it.value }
    }

    /** 몫 합이 작은 달부터 남은 예산을 남은 달 수로 똑같이 나눠 주고, 다 못 쓴 예산은 다음 달들에 넘긴다. */
    private fun monthShares(budget: Int): Map<YearMonth, Int> {
        val ascending = values.groupBy { it.month }.mapValues { (_, candidates) -> candidates.sumOf { it.cost } }
            .entries.sortedBy { it.value }
        return ascending.foldIndexed(emptyMap<YearMonth, Int>()) { index, shares, (month, total) ->
            val remaining = budget.coerceAtLeast(0) - shares.values.sum()
            shares + (month to minOf(total, remaining / (ascending.size - index)))
        }
    }

    /** 한 달 후보 가운데 최신부터 몫 합이 [share]를 넘기 전까지 이어진 구간과, 그보다 앞선 몫 0 후보. 시각순이다. */
    private fun newestWithin(candidates: List<SpreadCandidate<T>>, share: Int): List<SpreadCandidate<T>> {
        val kept = candidates.asReversed().runningFold(0) { total, candidate -> total + candidate.cost }
            .drop(1)
            .count { total -> total <= share }
        val firstKept = candidates.size - kept
        return candidates.filterIndexed { index, candidate -> index >= firstKept || candidate.cost == 0 }
    }
}
