package com.santacut.domain.analysis.model

import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 대화문에서 빼는 첨부·삭제 안내와 뜻을 잃지 않는 본문 정규화를 확인한다.
 */
class ScoringMessageTest {
    private val noon = LocalDateTime.of(2026, 11, 14, 12, 0)

    @Test
    fun `첨부와 삭제 안내만 있는 메시지는 자리표시다`() {
        listOf("사진", "사진 3장", "동영상", "이모티콘", "파일: 자료.pdf", "삭제된 메시지입니다.", " 사진 ").forEach {
            assertTrue(message(it).isPlaceholder(), it)
        }
        listOf("사진 보내줘", "고마워", "동영상 봤어?").forEach { assertFalse(message(it).isPlaceholder(), it) }
    }

    @Test
    fun `반복 글자는 ㅋㅎ 세 개, ㅠㅜ와 문장 부호 두 개까지 줄이고 줄바꿈과 겹친 공백은 공백 하나로 바꾼다`() {
        assertEquals("ㅋㅋㅋ 진짜?? 고마워ㅠㅠ !!", message("ㅋㅋㅋㅋㅋㅋ 진짜???? 고마워ㅠㅠㅠㅠ\n\n  !!!!").normalizedText())
        assertEquals("ㅋㅋ 응", message("  ㅋㅋ   응 ").normalizedText())
    }

    @Test
    fun `링크는 (링크)로 바꾼다`() {
        assertEquals("이거 봐 (링크) 대박", message("이거 봐 https://example.com/a?b=1 대박").normalizedText())
        assertEquals("(링크) (링크)", message("www.example.com HTTP://EXAMPLE.COM").normalizedText())
    }

    @Test
    fun `잇단 이모지는 두 개까지만 남긴다`() {
        assertEquals("축하해 🎉🎉 최고 😂😂", message("축하해 🎉🎉🎉🎉 최고 😂😂😂").normalizedText())
        assertEquals("좋아 ❤️❤️", message("좋아 ❤️❤️❤️").normalizedText())
        assertEquals("응 👍", message("응 👍").normalizedText())
    }

    @Test
    fun `300자를 넘는 본문은 앞부분만 두고 (긴 글 생략)을 붙인다`() {
        assertEquals("가".repeat(299) + " (긴 글 생략)", message("가".repeat(299) + "😀" + "나".repeat(50)).normalizedText())
        assertEquals("가".repeat(300), message("가".repeat(300)).normalizedText())
    }

    @Test
    fun `화자를 주지 않으면 본인이다`() {
        assertTrue(message("고마워").isSelf())
        assertFalse(ScoringMessage(noon, "고마워", "상대").isSelf())
    }

    private fun message(text: String): ScoringMessage = ScoringMessage(noon, text)
}
