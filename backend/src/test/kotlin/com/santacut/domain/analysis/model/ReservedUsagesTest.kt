package com.santacut.domain.analysis.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * 진행 중인 작업의 어림 사용량을 더하고, 끝난 작업의 예약을 하나씩 푸는지 확인한다.
 */
class ReservedUsagesTest {
    @Test
    fun `예약한 어림 사용량의 입력과 출력 토큰을 모두 더한다`() {
        val reservations = ReservedUsages()
        reservations.add(ModelUsage(INPUT_TOKENS, OUTPUT_TOKENS, ESTIMATE))
        reservations.add(ModelUsage(INPUT_TOKENS, OUTPUT_TOKENS, ESTIMATE))

        val total = reservations.total()

        assertEquals(INPUT_TOKENS * 2, total.inputTokens)
        assertEquals(OUTPUT_TOKENS * 2, total.outputTokens)
    }

    @Test
    fun `같은 어림 사용량을 둘 예약했으면 하나를 풀어도 나머지 하나는 남는다`() {
        val estimate = ModelUsage(INPUT_TOKENS, OUTPUT_TOKENS, ESTIMATE)
        val reservations = ReservedUsages()
        reservations.add(estimate)
        reservations.add(estimate)

        reservations.remove(estimate)

        assertEquals(INPUT_TOKENS, reservations.total().inputTokens)
    }

    @Test
    fun `예약이 없으면 합계는 사용량 없음이다`() {
        assertEquals(ModelUsage.NONE, ReservedUsages().total())
    }

    companion object {
        private const val INPUT_TOKENS = 40_000L
        private const val OUTPUT_TOKENS = 250L
        private const val ESTIMATE = "ESTIMATE"
    }
}
