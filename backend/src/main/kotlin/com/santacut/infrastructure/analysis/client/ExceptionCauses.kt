package com.santacut.infrastructure.analysis.client

/**
 * 예외와 그 원인을 바깥에서 안쪽 순서로 늘어놓은 목록. Spring AI가 감싼 AWS SDK 예외를 찾을 때 쓴다.
 */
class ExceptionCauses private constructor(private val values: List<Throwable>) {
    /** 권한 거부, 요청 한도, 할당량, 서비스 일시 중단처럼 잠시 뒤 다시 해야 할 제공자 오류가 들어 있는지. */
    fun isProviderUnavailable(): Boolean = values.any { it.javaClass.simpleName in PROVIDER_UNAVAILABLE_TYPES }

    companion object {
        private val PROVIDER_UNAVAILABLE_TYPES = setOf(
            "AccessDeniedException",
            "ThrottlingException",
            "ServiceQuotaExceededException",
            "ServiceUnavailableException",
        )

        fun of(exception: Throwable): ExceptionCauses =
            ExceptionCauses(generateSequence(exception) { it.cause.takeUnless { cause -> cause === it } }.toList())
    }
}
