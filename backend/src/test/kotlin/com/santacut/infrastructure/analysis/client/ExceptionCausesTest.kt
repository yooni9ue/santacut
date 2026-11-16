package com.santacut.infrastructure.analysis.client

import com.google.genai.errors.ClientException
import com.google.genai.errors.ServerException
import org.junit.jupiter.api.Test
import java.net.SocketTimeoutException
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExceptionCausesTest {
    @Test
    fun `감싼 예외 안쪽에 요청 한도 예외가 있으면 제공자 오류다`() {
        assertTrue(ExceptionCauses.of(IllegalStateException("감쌈", ClientException(429, "RESOURCE_EXHAUSTED", "한도"))).isProviderUnavailable())
        assertTrue(ExceptionCauses.of(ClientException(429, "RESOURCE_EXHAUSTED", "한도")).isProviderUnavailable())
    }

    @Test
    fun `서버 오류와 시간 초과도 제공자 오류다`() {
        assertTrue(ExceptionCauses.of(ServerException(503, "UNAVAILABLE", "과부하")).isProviderUnavailable())
        assertTrue(ExceptionCauses.of(RuntimeException("감쌈", SocketTimeoutException("timeout"))).isProviderUnavailable())
    }

    @Test
    fun `그 밖의 예외는 제공자 오류가 아니다`() {
        assertFalse(ExceptionCauses.of(IllegalStateException("AI 장애", RuntimeException())).isProviderUnavailable())
        assertFalse(ExceptionCauses.of(ClientException(400, "INVALID_ARGUMENT", "잘못된 요청")).isProviderUnavailable())
    }
}
