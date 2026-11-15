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
