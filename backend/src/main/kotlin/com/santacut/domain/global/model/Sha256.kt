package com.santacut.domain.global.model

import java.security.MessageDigest

/**
 * SHA-256 해시를 소문자 16진수 문자열로 만든다.
 */
object Sha256 {
    private const val ALGORITHM = "SHA-256"
    private const val HEX_BYTE_FORMAT = "%02x"

    fun hex(value: String): String = hex(value.toByteArray())

    fun hex(bytes: ByteArray): String =
        MessageDigest.getInstance(ALGORITHM)
            .digest(bytes)
            .joinToString("") { HEX_BYTE_FORMAT.format(it) }
}
