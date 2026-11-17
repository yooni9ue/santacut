package com.santacut.infrastructure.analysis.client

import com.google.genai.errors.ApiException
import java.io.InterruptedIOException
import java.net.SocketException
import java.net.UnknownHostException

/**
 * 예외와 그 원인을 바깥에서 안쪽 순서로 늘어놓은 목록. Spring AI가 감싼 Google GenAI SDK 예외를 찾을 때 쓴다.
 */
class ExceptionCauses private constructor(private val values: List<Throwable>) {
    /**
     * 요청 한도(429), 서버 오류(5xx), 시간 초과, 연결 실패처럼 잠시 뒤 다시 해야 할 제공자 오류나,
     * 설정 오류([misconfiguredCode])가 들어 있는지. 어느 쪽이든 작업은 실패시키지 않고 대기열에 남긴다.
     */
    fun isProviderUnavailable(): Boolean = values.any { isUnavailable(it) } || misconfiguredCode() != null

    /**
     * API 키가 틀렸거나 만료됐거나(400 API 키 오류, 401), 권한이 없거나(403), 모델 이름이 틀린(404) 것처럼
     * 운영자가 설정을 고쳐야 하는 제공자 오류의 HTTP 상태 코드. 없으면 null이다.
     * 제공자는 키 오류를 400으로도 돌려주므로 오류 메시지([ApiException.message])로 키 오류만 가려낸다. 그 밖의 400은 요청 탓 실패다.
     */
    fun misconfiguredCode(): Int? = values.filterIsInstance<ApiException>().firstOrNull { isMisconfigured(it) }?.code()

    private fun isUnavailable(cause: Throwable): Boolean = when (cause) {
        is ApiException -> cause.code() == TOO_MANY_REQUESTS || cause.code() >= SERVER_ERROR_MIN
        is InterruptedIOException, is SocketException, is UnknownHostException -> true
        else -> false
    }

    private fun isMisconfigured(cause: ApiException): Boolean =
        cause.code() in CONFIGURATION_ERROR_CODES || (cause.code() == BAD_REQUEST && isApiKeyMessage(cause.message().orEmpty()))

    private fun isApiKeyMessage(message: String): Boolean = API_KEY_MARKERS.any { marker -> message.contains(marker, ignoreCase = true) }

    companion object {
        private const val BAD_REQUEST = 400
        private const val TOO_MANY_REQUESTS = 429
        private const val SERVER_ERROR_MIN = 500
        private val CONFIGURATION_ERROR_CODES = setOf(401, 403, 404)

        /** Gemini가 키 오류 400에 싣는 메시지(`API key not valid`, `API key expired`)와 이유 코드(`API_KEY_INVALID`). */
        private val API_KEY_MARKERS = listOf("API key", "API_KEY")

        fun of(exception: Throwable): ExceptionCauses =
            ExceptionCauses(generateSequence(exception) { it.cause.takeUnless { cause -> cause === it } }.toList())
    }
}
