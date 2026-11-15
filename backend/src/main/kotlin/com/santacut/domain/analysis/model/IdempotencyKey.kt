package com.santacut.domain.analysis.model

import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException

/**
 * 같은 분석 요청의 재전송을 알아보는 키.
 */
class IdempotencyKey private constructor(val value: String) {
    companion object {
        private val PATTERN = Regex("[A-Za-z0-9_-]{16,100}")

        fun from(raw: String): IdempotencyKey {
            if (!PATTERN.matches(raw)) throw SantaCutException(ErrorCode.INVALID_IDEMPOTENCY_KEY)
            return IdempotencyKey(raw)
        }
    }
}
