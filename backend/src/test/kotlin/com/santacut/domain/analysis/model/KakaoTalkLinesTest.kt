package com.santacut.domain.analysis.model

import com.santacut.ErrorCodeAssertions.assertErrorCode
import com.santacut.domain.global.exception.ErrorCode
import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KakaoTalkLinesTest {
    @ParameterizedTest
    @ValueSource(strings = ["kakao-pc.txt", "kakao-ios.txt", "kakao-android.txt"])
    fun `PC, iOS, Android 내보내기에서 같은 메시지를 읽는다`(fixture: String) {
        val messages = KakaoTalkLines.from(fixtureText(fixture)).toMessages()
        assertEquals(EXPECTED_MESSAGES, messages.toList())
    }

    @Test
    fun `오전 12시는 0시, 오후 12시는 12시로 읽는다`() {
        val text = """
            2026. 11. 14. 오전 12:00, 산타 : 자정
            2026. 11. 14. 오후 12:00, 산타 : 정오
        """.trimIndent()
        val times = KakaoTalkLines.from(text).toMessages().toList().map { it.occurredAt }
        assertEquals(listOf(Instant.parse("2026-11-13T15:00:00Z"), Instant.parse("2026-11-14T03:00:00Z")), times)
    }

    @Test
    fun `첫 메시지 줄의 형식으로 정하고 다른 형식의 줄은 앞 메시지 본문에 잇는다`() {
        val text = """
            --------------- 2026년 11월 14일 토요일 ---------------
            [산타] [오전 9:00] 첫 줄
            2026. 11. 14. 오전 9:01, 루돌프 : 인용한 줄
        """.trimIndent()
        val messages = KakaoTalkLines.from(text).toMessages().toList()
        assertEquals(1, messages.size)
        assertEquals("첫 줄\n2026. 11. 14. 오전 9:01, 루돌프 : 인용한 줄", messages.single().text)
    }

    @Test
    fun `날짜 구분선 전의 PC 메시지 줄과 첫 메시지 전의 줄은 버린다`() {
        val text = """
            [산타] [오전 9:00] 날짜 모름
            --------------- 2026년 11월 14일 토요일 ---------------
            [산타] [오전 9:01] 날짜 있음
        """.trimIndent()
        val messages = KakaoTalkLines.from(text).toMessages().toList()
        assertEquals(listOf("날짜 있음"), messages.map { it.text })
    }

    @Test
    fun `있을 수 없는 날짜인 줄은 메시지로 읽지 않고 시스템 줄처럼 버린다`() {
        val text = """
            2026. 11. 14. 오전 9:00, 산타 : 정상
            2026. 13. 40. 오전 9:00, 산타 : 이상한 날짜
        """.trimIndent()
        val messages = KakaoTalkLines.from(text).toMessages().toList()
        assertEquals(listOf("정상"), messages.map { it.text })
    }

    @Test
    fun `메시지를 하나도 읽지 못하면 UNSUPPORTED_FILE_FORMAT이다`() {
        val lines = KakaoTalkLines.from(fixtureText("not-chat.txt"))
        assertErrorCode(ErrorCode.UNSUPPORTED_FILE_FORMAT) { lines.toMessages() }
    }

    @Test
    fun `줄이 상한보다 많으면 줄바꿈뿐인 파일이어도 읽기 전에 UNSUPPORTED_FILE_FORMAT이다`() {
        val text = "2026. 11. 14. 오전 9:00, 산타 : 시작" + "\n".repeat(KakaoTalkLines.MAX_LINES)
        assertErrorCode(ErrorCode.UNSUPPORTED_FILE_FORMAT) { KakaoTalkLines.from(text) }
    }

    @Test
    fun `이어진 줄이 수만 개여도 본문은 이벤트 본문 상한까지만 모으고 금방 끝난다`() {
        val text = "2026. 11. 14. 오전 9:00, 산타 : 시작\n" + "이어진줄".repeat(10).plus("\n").repeat(CONTINUATION_LINES)
        val message = assertTimeoutPreemptively<ChatMessage>(Duration.ofSeconds(5)) { KakaoTalkLines.from(text).toMessages().toList().single() }
        assertEquals(AnalysisEvent.MAX_TEXT_LENGTH, message.text.length)
        assertTrue(message.text.startsWith("시작\n이어진줄"))
    }

    private fun fixtureText(name: String): String {
        val stream = checkNotNull(javaClass.getResourceAsStream("/fixtures/upload/$name"))
        return RawChatFile(DataSource.KAKAO, stream.use { it.readBytes() }).decodeText()
    }

    companion object {
        private const val CONTINUATION_LINES = 80_000

        private val EXPECTED_MESSAGES = listOf(
            ChatMessage("산타", Instant.parse("2026-11-13T15:05:00Z"), "다들 잘 자"),
            ChatMessage("루돌프", Instant.parse("2026-11-14T00:30:00Z"), "좋은 아침"),
            ChatMessage("산타", Instant.parse("2026-11-14T03:00:00Z"), "점심 같이 먹자\n둘째 줄"),
            ChatMessage("산타 클로스", Instant.parse("2026-11-14T04:20:00Z"), "비율 3 : 2"),
            ChatMessage("산타", Instant.parse("2026-11-14T14:59:00Z"), "자정 직전"),
            ChatMessage("산타", Instant.parse("2026-11-14T23:00:00Z"), "다음 날 아침"),
        )
    }
}
