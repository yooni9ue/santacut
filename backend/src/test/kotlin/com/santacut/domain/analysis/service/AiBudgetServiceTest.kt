package com.santacut.domain.analysis.service

import com.santacut.ErrorCodeAssertions
import com.santacut.domain.analysis.model.AiCallKind
import com.santacut.domain.analysis.model.ModelUsage
import com.santacut.domain.global.config.AiBudgetProperties
import com.santacut.domain.global.exception.ErrorCode
import com.santacut.infrastructure.analysis.repository.AiUsageDayRepository
import com.santacut.infrastructure.global.config.RoastModelProperties
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.math.BigDecimal
import java.time.Instant

/**
 * 호출 전 비용 예약이 호출 종류마다 그 모델 단가로 센 예약 비용을 합쳐 한도와 비교하는지 확인한다. 쓴 사용량은 없다(빈 저장소).
 * 한 달 한도 1달러. 채점 입력 200만 토큰은 0.6달러(0.3달러 단가), 작가 입력 30만 토큰은 0.45달러(1.5달러 단가)다.
 */
class AiBudgetServiceTest {
    private val repository: AiUsageDayRepository = mock(AiUsageDayRepository::class.java)
    private val service = AiBudgetService(repository, AiBudgetProperties(monthlyUsd = BigDecimal.ONE), RoastModelProperties())

    @Test
    fun `채점 예약과 작가 예약을 각 단가로 센 합이 한도를 넘으면 막는다`() {
        service.reserve(AiCallKind.SCORE, SCORE_ESTIMATE, NOW)

        ErrorCodeAssertions.assertErrorCode(ErrorCode.AI_BUDGET_EXCEEDED) { service.reserve(AiCallKind.ROAST, ROAST_ESTIMATE, NOW) }
    }

    @Test
    fun `같은 토큰이라도 채점 단가로 세면 한도 안이다`() {
        service.reserve(AiCallKind.SCORE, SCORE_ESTIMATE, NOW)

        service.reserve(AiCallKind.SCORE, ROAST_ESTIMATE, NOW)
    }

    @Test
    fun `채점 예약을 풀면 작가 예약이 한도 안에 들어간다`() {
        service.reserve(AiCallKind.SCORE, SCORE_ESTIMATE, NOW)
        service.release(AiCallKind.SCORE, SCORE_ESTIMATE)

        service.reserve(AiCallKind.ROAST, ROAST_ESTIMATE, NOW)
    }

    companion object {
        private val NOW: Instant = Instant.parse("2026-11-20T03:00:00Z")
        private val SCORE_ESTIMATE = ModelUsage(2_000_000, 0, "ESTIMATE")
        private val ROAST_ESTIMATE = ModelUsage(300_000, 0, "ESTIMATE")
    }
}
