package com.santacut.infrastructure.analysis.client

import com.google.genai.errors.ApiException
import java.io.InterruptedIOException
import java.net.SocketException
import java.net.UnknownHostException

/**
 * 예외와 그 원인을 바깥에서 안쪽 순서로 늘어놓은 목록. Spring AI가 감싼 Google GenAI SDK 예외를 찾을 때 쓴다.
 */
class ExceptionCauses private constructor(private val values: List<Throwable>) {
    /** 권한 거부(403), 요청 한도(429), 서버 오류(5xx), 시간 초과, 연결 실패처럼 잠시 뒤 다시 해야 할 제공자 오류가 들어 있는지. */
    fun isProviderUnavailable(): Boolean = values.any { isUnavailable(it) }

    private fun isUnavailable(cause: Throwable): Boolean = when (cause) {
        is ApiException -> cause.code() in UNAVAILABLE_CLIENT_CODES || cause.code() >= SERVER_ERROR_MIN
        is InterruptedIOException, is SocketException, is UnknownHostException -> true
        else -> false
    }

    companion object {
        private val UNAVAILABLE_CLIENT_CODES = setOf(403, 429)
        private const val SERVER_ERROR_MIN = 500

        fun of(exception: Throwable): ExceptionCauses =
            ExceptionCauses(generateSequence(exception) { it.cause.takeUnless { cause -> cause === it } }.toList())
    }
}
