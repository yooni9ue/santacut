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

    @Test
    fun `엄격한 UTF-8로 읽고 맨 앞 BOM을 뗀다`() {
        val content = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + "산타\r\n".toByteArray()
        assertEquals("산타\r\n", RawChatFile(DataSource.KAKAO, content).decodeText())
    }

    @Test
    fun `UTF-8이 아닌 파일은 UNSUPPORTED_FILE_FORMAT이다`() {
        val eucKr = "산타".toByteArray(charset("EUC-KR"))
        assertErrorCode(ErrorCode.UNSUPPORTED_FILE_FORMAT) { RawChatFile(DataSource.KAKAO, eucKr).decodeText() }
    }

    @Test
    fun `출처가 카카오톡이면 TXT 파서로, 인스타그램이면 넘긴 JSON 읽기 함수로 읽는다`() {
        val kakao = RawChatFile(DataSource.KAKAO, "2026. 11. 1. 15:00, 산타 : 안녕".toByteArray())
        assertEquals(listOf("안녕"), kakao.readMessages { error("카카오톡은 JSON으로 읽지 않는다") }.toList().map { it.text })

        val thread = InstagramThread(InstagramMessages(listOf(InstagramMessage("산타", 1, "고마워"))))
        val instagram = RawChatFile(DataSource.INSTAGRAM, "{}".toByteArray())
        assertEquals(listOf("고마워"), instagram.readMessages { text -> thread.also { assertEquals("{}", text) } }.toList().map { it.text })
    }

    companion object {
        private const val BINARY_CHECK_LENGTH = 4096
    }
}
