package com.santacut.infrastructure.analysis.client

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExceptionCausesTest {
    @Test
    fun `감싼 예외 안쪽에 요청 한도 예외가 있으면 제공자 오류다`() {
        assertTrue(ExceptionCauses.of(IllegalStateException("감쌈", ThrottlingException())).isProviderUnavailable())
        assertTrue(ExceptionCauses.of(ThrottlingException()).isProviderUnavailable())
    }

    @Test
    fun `그 밖의 예외는 제공자 오류가 아니다`() {
        assertFalse(ExceptionCauses.of(IllegalStateException("AI 장애", RuntimeException())).isProviderUnavailable())
    }
}
