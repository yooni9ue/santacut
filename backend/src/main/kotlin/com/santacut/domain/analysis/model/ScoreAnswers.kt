package com.santacut.domain.analysis.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * 같은 채점 구간을 여러 번 따로 물은 답 목록. 항목마다 중앙값을 내 모델 점수의 흔들림을 줄인다(self-consistency).
 *
 * - 한 번이라도 쓸 수 없는 답(제공자 차단, 읽기 실패)이나 형식·범위가 틀린 점수 답이 있으면 그 답을 그대로 돌려준다.
 *   작업은 원자적이라 그 답 하나로 작업 전체가 실패하거나 대기열로 돌아간다.
 * - 점수 답이 절반 이하면 근거 부족 답을 돌려준다.
 * - 점수(s), 기회 수(n), 장난기는 점수 답들의 항목별 중앙값이다. 근거 줄은 중앙값에 가장 가까운 답의 것을 쓴다.
 * - 돌려주는 답의 사용량은 모든 호출의 합이다.
 */
class ScoreAnswers(private val values: List<ScoreAnswer>) {
    /** 여러 답을 하나로 합친 답. 답이 하나면 그 답 그대로다. */
    fun median(): ScoreAnswer {
        if (values.size == 1) return values.single()
        val total = values.fold(ModelUsage.NONE) { sum, answer -> sum + answer.usage }
        val broken = values.firstOrNull { answer -> !answer.isUsable() || (answer.isScored() && !answer.isValidScore()) }
        if (broken != null) return broken.withUsage(total)
        val scored = values.filter { answer -> answer.isScored() }
        if (scored.size * 2 <= values.size) return values.first { answer -> !answer.isScored() }.withUsage(total)
        return ScoreAnswers(scored).medianOfScored(total)
    }

    /** 모두 올바른 점수 답일 때의 항목별 중앙값 답. */
    private fun medianOfScored(total: ModelUsage): ScoreAnswer {
        val positions = ScoreDimension.entries.indices
        val scores = positions.map { position -> medianDecimal(values.map { answer -> answer.scoreAt(position) }) }
        val closest = values.minBy { answer -> positions.sumOf { position -> (answer.scoreAt(position) - scores[position]).abs() } }
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

    private fun medianInt(numbers: List<Int>): Int = medianDecimal(numbers.map { BigDecimal(it) }).setScale(0, RoundingMode.HALF_UP).toInt()

    companion object {
        private val TWO = BigDecimal(2)

        /** 점수는 소수 둘째 자리까지라 짝수 개의 가운데 두 값 평균도 이 자릿수로 반올림한다. */
        private const val SCORE_SCALE = 2
        private const val MEDIAN_SHAPE = "median"

        /**
         * [asker]를 [count]번 동시에(가상 스레드) 불러 답을 모은다. 모든 호출이 끝난 뒤 돌아오므로, 한 호출이 예외를 던져도
         * 나머지 호출의 사용량 기록은 끝나 있다. 호출 예외는 감싸지 않고 그대로 던진다.
         */
        fun sampled(count: Int, asker: () -> ScoreAnswer): ScoreAnswers =
            Executors.newVirtualThreadPerTaskExecutor().use { executor ->
                val futures = List(count) { executor.submit(Callable { asker() }) }
                ScoreAnswers(futures.map { future -> resultOf(future) })
            }

        private fun resultOf(future: Future<ScoreAnswer>): ScoreAnswer =
            try {
                future.get()
            } catch (exception: ExecutionException) {
                throw exception.cause ?: exception
            }
    }
}
