package com.santacut.infrastructure.analysis.client

import com.google.genai.errors.ClientException
import com.google.genai.errors.ServerException
import org.junit.jupiter.api.Test
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
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
    fun `연결 실패와 호스트 조회 실패도 제공자 오류다`() {
        assertTrue(ExceptionCauses.of(RuntimeException("감쌈", ConnectException("Connection refused"))).isProviderUnavailable())
        assertTrue(ExceptionCauses.of(RuntimeException("감쌈", UnknownHostException("generativelanguage.googleapis.com"))).isProviderUnavailable())
    }

    @Test
    fun `API 키 오류 400과 401·403·404는 설정 오류라 제공자 오류로 보고 상태 코드를 알려 준다`() {
        val invalidKey = ClientException(400, "Bad Request", "API key not valid. Please pass a valid API key.")
        assertEquals(400, ExceptionCauses.of(IllegalStateException("감쌈", invalidKey)).misconfiguredCode())
        assertTrue(ExceptionCauses.of(invalidKey).isProviderUnavailable())
        listOf(401, 403, 404).forEach { code ->
            val causes = ExceptionCauses.of(ClientException(code, "설정", "설정 오류"))
            assertEquals(code, causes.misconfiguredCode())
            assertTrue(causes.isProviderUnavailable())
        }
    }

    @Test
    fun `그 밖의 예외와 요청 탓 400은 제공자 오류가 아니다`() {
        assertFalse(ExceptionCauses.of(IllegalStateException("AI 장애", RuntimeException())).isProviderUnavailable())
        val badRequest = ExceptionCauses.of(ClientException(400, "INVALID_ARGUMENT", "잘못된 요청"))
        assertFalse(badRequest.isProviderUnavailable())
        assertNull(badRequest.misconfiguredCode())
        assertNull(ExceptionCauses.of(ClientException(429, "RESOURCE_EXHAUSTED", "한도")).misconfiguredCode())
    }
}
