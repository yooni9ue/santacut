package com.santacut.domain.model

import com.santacut.domain.exception.ErrorCode
import com.santacut.domain.exception.SantaCutException

/**
 * 공개 공유 링크 토큰(43자 base64url). 형식이 틀리면 없는 링크로 취급한다.
 */
class ShareToken private constructor(val value: String) {
    companion object {
        private val PATTERN = Regex("[A-Za-z0-9_-]{43}")

        fun from(raw: String): ShareToken {
            if (!PATTERN.matches(raw)) throw SantaCutException(ErrorCode.SHARE_NOT_FOUND)
            return ShareToken(raw)
        }
    }
}
