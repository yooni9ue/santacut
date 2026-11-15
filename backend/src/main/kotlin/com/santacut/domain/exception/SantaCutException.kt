package com.santacut.domain.exception

/**
 * 산타컷의 모든 비즈니스 오류.
 *
 * @property detail 로그에만 남기는 부가 정보. 사용자 입력이나 AI 응답 원문은 넣지 않는다.
 */
class SantaCutException(
    val errorCode: ErrorCode,
    val detail: String? = null,
) : RuntimeException(errorCode.message) {
    /** 로그에 남길 사유. */
    fun logReason(): String = detail?.let { "${errorCode.name}:$it" } ?: errorCode.name
}
