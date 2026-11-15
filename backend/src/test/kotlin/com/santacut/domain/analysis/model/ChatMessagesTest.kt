package com.santacut.domain.analysis.model

import com.santacut.ErrorCodeAssertions.assertErrorCode
import com.santacut.domain.global.exception.ErrorCode
import org.junit.jupiter.api.Test
import java.text.Normalizer
import java.time.Instant
import kotlin.test.assertEquals

class ChatMessagesTest {
    private val now = Instant.parse("2026-11-15T00:00:00Z")

    @Test
    fun `본인 메시지만 이벤트로 바꾸고 다른 참여자 메시지와 빈 본문은 버린다`() {
        val messages = ChatMessages(
            listOf(
                ChatMessage("산타", now, "고마워"),
                ChatMessage("루돌프", now, "남의 메시지"),
                ChatMessage("산타", now, "   "),
                ChatMessage("산타", now.plusSeconds(1), "힘내"),
            ),
        )
        assertEquals(listOf("고마워", "힘내"), messages.eventsWrittenBy("산타").toList().map { it.text })
    }

    @Test
    fun `본인 이름은 앞뒤 공백을 떼고 NFC로 맞춰 완전히 같을 때만 본인이다`() {
        val decomposed = Normalizer.normalize("산타", Normalizer.Form.NFD)
        val messages = ChatMessages(listOf(ChatMessage(decomposed, now, "고마워"), ChatMessage("산타클로스", now, "다른 사람")))
        assertEquals(listOf("고마워"), messages.eventsWrittenBy(" 산타 ").toList().map { it.text })
    }

    @Test
    fun `본인 메시지가 하나도 없으면 SPEAKER_NOT_IDENTIFIED다`() {
        val messages = ChatMessages(listOf(ChatMessage("루돌프", now, "안녕")))
        assertErrorCode(ErrorCode.SPEAKER_NOT_IDENTIFIED) { messages.eventsWrittenBy("산타") }
    }

    @Test
    fun `2,000자보다 긴 본문은 2,000자로 자른다`() {
        val messages = ChatMessages(listOf(ChatMessage("산타", now, "가".repeat(AnalysisEvent.MAX_TEXT_LENGTH + 1))))
        assertEquals(AnalysisEvent.MAX_TEXT_LENGTH, messages.eventsWrittenBy("산타").toList().single().text.length)
    }

    @Test
    fun `시각순 정렬은 같은 시각 메시지의 원래 순서를 지킨다`() {
        val messages = ChatMessages(
            listOf(
                ChatMessage("산타", now.plusSeconds(1), "셋째"),
                ChatMessage("산타", now, "첫째"),
                ChatMessage("산타", now, "둘째"),
            ),
        )
        assertEquals(listOf("첫째", "둘째", "셋째"), messages.sortedByTime().toList().map { it.text })
    }

    @Test
    fun `메시지가 없으면 UNSUPPORTED_FILE_FORMAT이다`() {
        assertErrorCode(ErrorCode.UNSUPPORTED_FILE_FORMAT) { ChatMessages(emptyList()).verifyNotEmpty() }
    }
}
