package com.santacut.domain.model

import com.santacut.domain.exception.ErrorCode
import com.santacut.domain.exception.SantaCutException
import java.text.Normalizer
import java.util.Locale

/**
 * NFKC로 정규화한 닉네임. 중복 검사는 대소문자를 구분하지 않는 [key]로 한다.
 */
class Nickname private constructor(val value: String) {
    fun key(): String = value.lowercase(Locale.ROOT)

    companion object {
        private val PATTERN = Regex("[가-힣A-Za-z0-9_]{2,16}")

        fun from(raw: String): Nickname {
            val normalized = Normalizer.normalize(raw.trim(), Normalizer.Form.NFKC)
            if (!PATTERN.matches(normalized)) throw SantaCutException(ErrorCode.INVALID_NICKNAME)
            return Nickname(normalized)
        }
    }
}
