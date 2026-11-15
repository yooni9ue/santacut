package com.santacut.domain.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class Sha256Test {
    @Test
    fun `표준 시험 벡터와 같은 소문자 16진수 해시를 만든다`() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", Sha256.hex("abc"))
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", Sha256.hex(""))
    }

    @Test
    fun `해시는 64자이고 입력이 다르면 달라진다`() {
        assertEquals(SHA256_HEX_LENGTH, Sha256.hex("산타컷").length)
        assertNotEquals(Sha256.hex("token-a"), Sha256.hex("token-b"))
    }

    companion object {
        private const val SHA256_HEX_LENGTH = 64
    }
}
