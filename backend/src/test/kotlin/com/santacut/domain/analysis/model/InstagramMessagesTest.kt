package com.santacut.domain.analysis.model

import com.santacut.ErrorCodeAssertions.assertErrorCode
import com.santacut.domain.global.exception.ErrorCode
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.Instant
import kotlin.test.assertEquals

class InstagramMessagesTest {
    private val mapper = jacksonObjectMapper()

    @Test
    fun `본문이 있는 메시지만 깨진 글자를 되살려 시각순으로 읽는다`() {
        val stream = checkNotNull(javaClass.getResourceAsStream("/fixtures/upload/instagram-message_1.json"))
        val text = RawChatFile(DataSource.INSTAGRAM, stream.use { it.readBytes() }).decodeText()

        val messages = mapper.readValue(text, InstagramThread::class.java).toMessages()

        val expected = listOf(
            ChatMessage("Santa", Instant.ofEpochMilli(1763118000000), "plain ascii"),
            ChatMessage("산타", Instant.ofEpochMilli(1763128800000), "오늘도 힘내"),
            ChatMessage("루돌프", Instant.ofEpochMilli(1763132400000), "고마워 🎄"),
        )
        assertEquals(expected, messages.toList())
    }

    @Test
    fun `messages 배열이 없거나 본문 있는 메시지가 없으면 UNSUPPORTED_FILE_FORMAT이다`() {
        val withoutMessages = mapper.readValue("{\"participants\":[]}", InstagramThread::class.java)
        val withoutContent = mapper.readValue(
            "{\"messages\":[{\"sender_name\":\"a\",\"timestamp_ms\":1}]}",
            InstagramThread::class.java,
        )
        assertErrorCode(ErrorCode.UNSUPPORTED_FILE_FORMAT) { withoutMessages.toMessages() }
        assertErrorCode(ErrorCode.UNSUPPORTED_FILE_FORMAT) { withoutContent.toMessages() }
    }

    @Test
    fun `messages 배열에 null 원소가 있으면 UNSUPPORTED_FILE_FORMAT이다`() {
        val withNull = mapper.readValue(
            "{\"messages\":[null,{\"sender_name\":\"a\",\"timestamp_ms\":1,\"content\":\"b\"}]}",
            InstagramThread::class.java,
        )
        assertErrorCode(ErrorCode.UNSUPPORTED_FILE_FORMAT) { withNull.toMessages() }
    }
}
