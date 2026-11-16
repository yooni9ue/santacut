package com.santacut.domain.analysis.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 같은 채점 구간을 여러 번 따로 물은 답 목록. 항목마다 중앙값을 내 모델 점수의 흔들림을 줄인다(self-consistency).
 *
 * - 쓸 수 없는 답(제공자 차단, 읽기 실패, 호출 예외)과 형식·범위가 틀린 점수 답은 버린다.
 *   남은 답이 과반이 못 되면 버린 답을 돌려준다. 제공자 차단 답이 있으면 그 답을, 없으면 첫 답을 고른다.
 *   그래서 제공자 차단은 늘 대기열로 돌아가고 영구 실패가 되지 않는다. 작업은 원자적이라 그 답 하나로 작업 전체가 실패하거나 대기열로 돌아간다.
 * - 남은 답 가운데 점수 답이 과반이 못 되면 근거 부족 답을 돌려준다.
 * - 점수(s), 기회 수(n), 장난기는 점수 답들의 항목별 중앙값이다. 근거 줄은 중앙값에 가장 가까운 답의 것을 쓴다.
 * - 돌려주는 답의 사용량은 버린 답까지 모든 호출의 합이다.
 */
class ScoreAnswers(private val values: List<ScoreAnswer>) {
    /** 여러 답을 하나로 합친 답. 답이 하나면 그 답 그대로다. */
    fun median(): ScoreAnswer {
        if (values.size == 1) return values.single()
        val total = values.fold(ModelUsage.NONE) { sum, answer -> sum + answer.usage }
        val valid = values.filter { answer -> isValid(answer) }
        if (!isMajority(valid.size, values.size)) return discarded().withUsage(total)
        val scored = valid.filter { answer -> answer.isScored() }
        if (!isMajority(scored.size, valid.size)) return valid.first { answer -> !answer.isScored() }.withUsage(total)
        return ScoreAnswers(scored).medianOfScored(total)
    }

    /** 과반이 못 될 때 돌려줄 버린 답. 제공자 차단 답을 먼저 골라 작업이 대기열로 돌아가게 한다. */
    private fun discarded(): ScoreAnswer =
        values.firstOrNull { answer -> answer.isProviderUnavailable() } ?: values.first { answer -> !isValid(answer) }

    /** 모두 올바른 점수 답일 때의 항목별 중앙값 답. */
    private fun medianOfScored(total: ModelUsage): ScoreAnswer {
        val positions = ScoreDimension.entries.indices
        val scores = positions.map { position -> medianDecimal(values.map { answer -> answer.scoreAt(position) }) }
        val closest = values.minBy { answer ->
            positions.sumOf { position -> (answer.scoreAt(position) - scores[position]).abs() }
        }
        return ScoreAnswer.of(
            scores,
            positions.map { position -> closest.evidenceAt(position) },
            MEDIAN_SHAPE,
            total,
            positions.map { position -> medianInt(values.map { answer -> answer.opportunityAt(position) }) },
            values.mapNotNull { answer -> answer.playfulness }.takeIf { it.isNotEmpty() }?.let { medianInt(it) },
        )
    }

    private fun medianDecimal(numbers: List<BigDecimal>): BigDecimal {
        val sorted = numbers.sorted()
        val middle = sorted.size / 2
        if (sorted.size % 2 == 1) return sorted[middle]
        return (sorted[middle - 1] + sorted[middle]).divide(TWO, SCORE_SCALE, RoundingMode.HALF_UP)
    }

    private fun medianInt(numbers: List<Int>): Int =
        medianDecimal(numbers.map { BigDecimal(it) }).setScale(INTEGER_SCALE, RoundingMode.HALF_UP).toInt()

    companion object {
        private val TWO = BigDecimal(2)

        /** 점수는 소수 둘째 자리까지라 짝수 개의 가운데 두 값 평균도 이 자릿수로 반올림한다. */
        private const val SCORE_SCALE = 2

        /** 기회 수와 장난기는 정수라 중앙값을 소수점 없이 반올림한다. */
        private const val INTEGER_SCALE = 0

        /** 과반 판정. [count]에 이 값을 곱해 전체보다 커야 과반이다(3개 중 2개, 4개 중 3개). */
        private const val MAJORITY_FACTOR = 2
        private const val MEDIAN_SHAPE = "median"

        /** 버리지 않는 답: 쓸 수 있고, 점수 답이면 형식·범위가 맞다. */
        private fun isValid(answer: ScoreAnswer): Boolean = answer.isUsable() && (!answer.isScored() || answer.isValidScore())

        private fun isMajority(count: Int, total: Int): Boolean = count * MAJORITY_FACTOR > total
    }
}
