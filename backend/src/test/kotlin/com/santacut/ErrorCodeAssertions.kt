package com.santacut

import com.santacut.domain.exception.ErrorCode
import com.santacut.domain.exception.SantaCutException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * 비즈니스 오류 검증 보조. 동작이 [SantaCutException]을 던지고 그 오류 코드가 기대값인지 확인한다.
 */
object ErrorCodeAssertions {
    fun assertErrorCode(expected: ErrorCode, action: () -> Unit) {
        val exception = assertFailsWith<SantaCutException> { action() }
        assertEquals(expected, exception.errorCode)
    }
}
