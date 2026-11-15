package com.santacut.domain.analysis.model

import com.santacut.ErrorCodeAssertions.assertErrorCode
import com.santacut.domain.global.exception.ErrorCode
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class RawChatFileTest {
    @Test
    fun `대화 텍스트는 받는다`() {
        RawChatFile(DataSource.KAKAO, "2026. 11. 1. 오후 3:00, 산타 : 안녕".toByteArray()).verifyReceivable()
    }

    @Test
    fun `빈 파일, zip 파일, 앞부분에 NUL이 있는 파일은 UNSUPPORTED_FILE_FORMAT이다`() {
        val zip = byteArrayOf(0x50, 0x4B, 0x03, 0x04) + "messages".toByteArray()
        val binary = "abc".toByteArray() + byteArrayOf(0) + "def".toByteArray()
        listOf(ByteArray(0), zip, binary).forEach { content ->
            assertErrorCode(ErrorCode.UNSUPPORTED_FILE_FORMAT) { RawChatFile(DataSource.KAKAO, content).verifyReceivable() }
        }
    }

    @Test
    fun `4KB 뒤에 있는 NUL은 보지 않는다`() {
        val content = "가".repeat(BINARY_CHECK_LENGTH).toByteArray() + byteArrayOf(0)
        RawChatFile(DataSource.KAKAO, content).verifyReceivable()
    }

    @Test
    fun `요청 해시는 출처, 본인 이름, 내용이 모두 같을 때만 같다`() {
        val content = "대화".toByteArray()
        val hash = RawChatFile(DataSource.KAKAO, content).requestHash("산타")
        assertEquals(hash, RawChatFile(DataSource.KAKAO, content.copyOf()).requestHash("산타"))
        assertNotEquals(hash, RawChatFile(DataSource.INSTAGRAM, content).requestHash("산타"))
        assertNotEquals(hash, RawChatFile(DataSource.KAKAO, content).requestHash("루돌프"))
        assertNotEquals(hash, RawChatFile(DataSource.KAKAO, "대화!".toByteArray()).requestHash("산타"))
    }

    companion object {
        private const val BINARY_CHECK_LENGTH = 4096
    }
}
