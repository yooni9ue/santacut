package com.santacut.domain.global.model

import java.security.SecureRandom
import java.util.Base64

/**
 * 세션과 공유 링크에 쓰는 추측 불가능한 토큰.
 */
object RandomToken {
    private const val TOKEN_BYTES = 32
    private val random = SecureRandom()

    /** 32바이트 난수를 패딩 없는 base64url(43자)로 만든다. */
    fun generate(): String {
        val bytes = ByteArray(TOKEN_BYTES).also(random::nextBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
