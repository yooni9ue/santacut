package com.santacut.domain.global.model

import com.santacut.domain.analysis.model.IdempotencyKey
import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import com.santacut.domain.participant.model.Nickname
import com.santacut.domain.share.model.ShareToken
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * 요청 값으로 만드는 값 객체(닉네임, 멱등 키, 공유 토큰)의 검증 규칙.
 */
class RequestValueTest {
    @Test
    fun `닉네임은 앞뒤 공백을 지우고 NFKC로 정규화하며 비교 키는 소문자다`() {
        val nickname = Nickname.from("  ＳａｎｔａＫｉｍ  ")
        assertEquals("SantaKim", nickname.value)
        assertEquals("santakim", nickname.key())
        assertEquals("산타_도우미1", Nickname.from("산타_도우미1").value)
    }

    @Test
    fun `닉네임이 2~16자의 한글 영문 숫자 밑줄이 아니면 닉네임 오류다`() {
        listOf("가", "a".repeat(17), "산타 할아버지", "santa!", "").forEach { raw ->
            val exception = assertFailsWith<SantaCutException> { Nickname.from(raw) }
            assertEquals(ErrorCode.INVALID_NICKNAME, exception.errorCode)
        }
    }

    @Test
    fun `멱등 키는 영문 숫자 밑줄 하이픈으로 된 16~100자다`() {
        assertEquals("abc_DEF-0123456789", IdempotencyKey.from("abc_DEF-0123456789").value)
        listOf("short-key", "a".repeat(101), "invalid key with space").forEach { raw ->
            val exception = assertFailsWith<SantaCutException> { IdempotencyKey.from(raw) }
            assertEquals(ErrorCode.INVALID_IDEMPOTENCY_KEY, exception.errorCode)
        }
    }

    @Test
    fun `공유 토큰은 43자 base64url이 아니면 없는 링크로 본다`() {
        val token = RandomToken.generate()
        assertEquals(43, token.length)
        assertEquals(token, ShareToken.from(token).value)
        listOf("too-short", token + "a", token.dropLast(1) + "!").forEach { raw ->
            val exception = assertFailsWith<SantaCutException> { ShareToken.from(raw) }
            assertEquals(ErrorCode.SHARE_NOT_FOUND, exception.errorCode)
        }
    }
}
