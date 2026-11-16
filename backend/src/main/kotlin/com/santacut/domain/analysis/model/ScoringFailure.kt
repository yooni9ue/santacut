package com.santacut.domain.analysis.model

import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException

/**
 * 점수 산정이 실패한 종류. 작업을 어떻게 끝낼지와 작업자를 멈출지를 정한다.
 *
 * @property failureCode 작업을 끝낼 실패 이유. null이면 작업을 실패시키지 않고 대기열로 되돌린다.
 * @property pausesAtOnce 바로 작업 잡기를 멈추는지(제공자가 호출을 막음)
 * @property countsTowardPause 연속 실패로 세는지. 근거 부족은 입력 탓이라 세지 않는다.
 */
enum class ScoringFailure(
    val failureCode: AnalysisFailureCode?,
    val pausesAtOnce: Boolean,
    val countsTowardPause: Boolean,
) {
    PROVIDER_BLOCKED(null, true, false),
    BUDGET_EXCEEDED(null, true, false),
    INSUFFICIENT_EVIDENCE(AnalysisFailureCode.INSUFFICIENT_EVIDENCE, false, false),
    ANALYSIS_ERROR(AnalysisFailureCode.ANALYSIS_FAILED, false, true),
    ;

    companion object {
        /** 점수 산정 중 난 예외를 실패 종류로 바꾼다. */
        fun of(exception: Exception): ScoringFailure = when {
            (exception as? SantaCutException)?.errorCode == ErrorCode.AI_PROVIDER_UNAVAILABLE -> PROVIDER_BLOCKED
            (exception as? SantaCutException)?.errorCode == ErrorCode.AI_BUDGET_EXCEEDED -> BUDGET_EXCEEDED
            AnalysisFailureCode.fromScoringFailure(exception) == AnalysisFailureCode.INSUFFICIENT_EVIDENCE -> INSUFFICIENT_EVIDENCE
            else -> ANALYSIS_ERROR
        }
    }
}
