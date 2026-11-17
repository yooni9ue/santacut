package com.santacut.domain.analysis.model

import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException

/**
 * 점수 산정이 실패한 종류. 작업을 어떻게 끝낼지와 작업자를 멈출지를 정한다.
 *
 * @property failureCode 작업을 끝낼 실패 이유. null이면 작업을 실패시키지 않고 대기열로 되돌린다.
 * @property pausesAtOnce 바로 작업 잡기를 멈추는지(제공자가 호출을 막음)
 * @property countsTowardPause 연속 실패로 세는지. 근거 부족과 응답 형식·근거·점수 오류는 같은 입력이면 다시 나는 입력 탓 실패라
 *   세지 않는다. 한 참가자의 입력이 모두의 채점을 멈추게 하지 않기 위해서다.
 * @property cachesDigest 받은 답에서 난 이 실패를 채점 입력 해시와 함께 남기는지. 같은 입력이면 다시 물어도 같은 실패라
 *   ([AnalysisFailureCode.isReusableScoringFailure]) 같은 입력의 재제출이 AI를 다시 부르지 않게 한다.
 */
enum class ScoringFailure(
    val failureCode: AnalysisFailureCode?,
    val pausesAtOnce: Boolean,
    val countsTowardPause: Boolean,
    val cachesDigest: Boolean,
) {
    PROVIDER_BLOCKED(null, true, false, false),
    BUDGET_EXCEEDED(null, true, false, false),
    INSUFFICIENT_EVIDENCE(AnalysisFailureCode.INSUFFICIENT_EVIDENCE, false, false, true),
    INPUT_REJECTED(AnalysisFailureCode.ANALYSIS_FAILED, false, false, true),
    ANALYSIS_ERROR(AnalysisFailureCode.ANALYSIS_FAILED, false, true, false),
    ;

    companion object {
        /** 점수 산정 중 난 예외를 실패 종류로 바꾼다. */
        fun of(exception: Exception): ScoringFailure = when {
            (exception as? SantaCutException)?.errorCode == ErrorCode.AI_PROVIDER_UNAVAILABLE -> PROVIDER_BLOCKED
            (exception as? SantaCutException)?.errorCode == ErrorCode.AI_BUDGET_EXCEEDED -> BUDGET_EXCEEDED
            AnalysisFailureCode.fromScoringFailure(exception) == AnalysisFailureCode.INSUFFICIENT_EVIDENCE -> INSUFFICIENT_EVIDENCE
            AnalysisFailureCode.isReusableScoringFailure(exception) -> INPUT_REJECTED
            else -> ANALYSIS_ERROR
        }
    }
}
