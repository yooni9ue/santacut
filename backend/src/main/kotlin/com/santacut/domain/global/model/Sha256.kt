package com.santacut.domain.global.model

import java.security.MessageDigest

/**
 * SHA-256 해시를 소문자 16진수 문자열로 만든다.
 */
object Sha256 {
    private const val ALGORITHM = "SHA-256"
    private const val HEX_BYTE_FORMAT = "%02x"

    fun hex(value: String): String = hex(value.toByteArray())

    fun hex(bytes: ByteArray): String = toHex(MessageDigest.getInstance(ALGORITHM).digest(bytes))

    /** [head] 뒤에 [body]를 이어 붙인 값의 해시. 두 배열을 실제로 이어 붙이지 않는다. */
    fun hex(head: ByteArray, body: ByteArray): String {
        val digest = MessageDigest.getInstance(ALGORITHM)
        digest.update(head)
        return toHex(digest.digest(body))
    }

    private fun toHex(hash: ByteArray): String = hash.joinToString("") { HEX_BYTE_FORMAT.format(it) }
}
