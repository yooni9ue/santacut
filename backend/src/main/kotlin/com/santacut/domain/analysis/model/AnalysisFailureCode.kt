package com.santacut.domain.analysis.model

import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException

/**
 * 분석 작업이 끝내 실패한 이유. 상수 이름은 `error_code` 응답 값이다.
 *
 * @property resultingStatus 이 이유로 끝난 작업의 최종 상태
 */
enum class AnalysisFailureCode(val resultingStatus: AnalysisJobStatus) {
    SEASON_FROZEN(AnalysisJobStatus.FAILED),
    EXPIRED(AnalysisJobStatus.FAILED),
    RETRY_EXHAUSTED(AnalysisJobStatus.FAILED),
    INSUFFICIENT_EVIDENCE(AnalysisJobStatus.INSUFFICIENT_DATA),
    ANALYSIS_FAILED(AnalysisJobStatus.FAILED),
    UNSUPPORTED_FILE_FORMAT(AnalysisJobStatus.FAILED),
    SPEAKER_NOT_IDENTIFIED(AnalysisJobStatus.FAILED),
    INSUFFICIENT_EVENTS(AnalysisJobStatus.INSUFFICIENT_DATA),
    ;

    companion object {
        /** 점수 산정 중 난 예외를 실패 이유로 바꾼다. 근거 부족만 따로 구분한다. */
        fun fromScoringFailure(exception: Exception): AnalysisFailureCode =
            if (exception is SantaCutException && exception.errorCode == ErrorCode.INSUFFICIENT_EVIDENCE) {
                INSUFFICIENT_EVIDENCE
            } else {
                ANALYSIS_FAILED
            }

        /**
         * 같은 채점 입력이면 다시 물어도 결과가 크게 다르지 않을 실패인지. 근거 부족과 응답 형식·근거·점수 오류가 해당한다.
         * 이런 실패는 입력 해시를 남겨 같은 입력의 재제출이 AI를 다시 부르지 않게 한다. 호출 오류·시간 초과는 해당하지 않는다.
         */
        fun isReusableScoringFailure(exception: Exception): Boolean =
            (exception as? SantaCutException)?.errorCode in REUSABLE_SCORING_ERRORS

        private val REUSABLE_SCORING_ERRORS = setOf(
            ErrorCode.INSUFFICIENT_EVIDENCE,
            ErrorCode.AI_RESPONSE_INVALID,
            ErrorCode.AI_RESPONSE_SCHEMA_INVALID,
            ErrorCode.AI_EVIDENCE_INVALID,
            ErrorCode.AI_SCORE_OUT_OF_RANGE,
        )

        /** 원본 파일 검증 중 난 예외를 실패 이유로 바꾼다. 형식, 화자, 이벤트 부족 말고는 모두 분석 실패다. */
        fun fromValidationFailure(exception: Exception): AnalysisFailureCode =
            when ((exception as? SantaCutException)?.errorCode) {
                ErrorCode.UNSUPPORTED_FILE_FORMAT -> UNSUPPORTED_FILE_FORMAT
                ErrorCode.SPEAKER_NOT_IDENTIFIED -> SPEAKER_NOT_IDENTIFIED
                ErrorCode.INSUFFICIENT_EVENTS -> INSUFFICIENT_EVENTS
                else -> ANALYSIS_FAILED
            }
    }
}
