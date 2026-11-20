package com.santacut.application.analysis.runner

import com.santacut.application.global.Runner
import com.santacut.application.global.TransactionRunner
import com.santacut.domain.analysis.model.AiCallKind
import com.santacut.domain.analysis.model.ModelUsage
import com.santacut.domain.analysis.model.ScoringFailure
import com.santacut.domain.analysis.service.AiBudgetService
import java.time.Clock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * 채점 스레드들이 함께 쓰는 AI 비용 가드([AiBudgetService])를 잠금과 트랜잭션으로 감싸는 실행기.
 * 비용 확인·예약과 사용량 기록은 커밋까지 [budgetLock] 안에서 해, 두 스레드가 동시에 확인을 통과해 한도를 넘지 않게 한다.
 * 제공자 문제로 멈추는 상태(연속 실패, 잠시 멈춤)는 모든 스레드가 함께 쓴다.
 */
@Runner
class AiBudgetRunner(
    private val aiBudgetService: AiBudgetService,
    private val transaction: TransactionRunner,
    private val clock: Clock,
) {
    private val budgetLock = ReentrantLock()

    /** 비용 한도에 닿았거나 제공자 문제로 멈춘 동안이면 참. 작업은 대기열에 남고 오래 밀리면 만료가 정리한다. */
    fun isBlocked(): Boolean = transaction.call { aiBudgetService.isBlocked(clock.instant()) } ?: false

    /** 채점 호출을 채점 모델 단가로 예약·기록하며 돌린다. 방법은 [spending]과 같다. */
    fun <T> spendingOnScore(estimate: ModelUsage, calls: (record: (ModelUsage) -> Unit) -> T): T =
        spending(AiCallKind.SCORE, estimate, calls)

    /** 작가 호출을 작가 모델 단가로 예약·기록하며 돌린다. 방법은 [spending]과 같다. */
    fun <T> spendingOnRoast(estimate: ModelUsage, calls: (record: (ModelUsage) -> Unit) -> T): T =
        spending(AiCallKind.ROAST, estimate, calls)

    /**
     * [kind] 호출의 어림 사용량([estimate])을 그 모델 단가로 한도 안인지 확인·예약한 뒤 [calls]로 AI에 묻고, 끝나면(실패해도) 예약을 푼다.
     * [calls]가 받는 기록 함수는 호출 한 번의 사용량을 바로 더한다. 뒤 호출이 실패해도 이미 과금된 호출이 비용 가드에 잡힌다.
     * 예약을 푸는 일은 잠금과 트랜잭션 밖에서 한다.
     */
    private fun <T> spending(kind: AiCallKind, estimate: ModelUsage, calls: (record: (ModelUsage) -> Unit) -> T): T {
        underBudgetLock { aiBudgetService.reserve(kind, estimate, clock.instant()) }
        try {
            return calls { usage -> underBudgetLock { aiBudgetService.record(kind, usage, clock.instant()) } }
        } finally {
            aiBudgetService.release(kind, estimate)
        }
    }

    fun recordSuccess() = aiBudgetService.recordSuccess()

    /** 실패 종류에 따라 바로 멈추거나 연속 실패로 센다. */
    fun recordFailure(failure: ScoringFailure) = aiBudgetService.recordFailure(failure, clock.instant())

    /** 비용 확인·예약과 사용량 기록을 커밋까지 한 줄로 세운다. 다른 스레드가 커밋 전 합계를 읽고 한도를 넘기지 않게 한다. */
    private fun underBudgetLock(action: () -> Unit) = budgetLock.withLock { transaction.run(action) }
}
