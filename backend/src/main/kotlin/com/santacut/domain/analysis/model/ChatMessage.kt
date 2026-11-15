package com.santacut.domain.analysis.model

import java.time.Instant

/**
 * 원본 파일에서 읽은 메시지 하나. [speaker]는 참여자 이름이므로 응답, 로그, 예외에 넣지 않는다.
 */
data class ChatMessage(
    val speaker: String,
    val occurredAt: Instant,
    val text: String,
) {
    /** 메시지 줄 다음에 이어진 줄을 본문 새 줄로 붙인다. */
    fun continuedWith(line: String): ChatMessage = copy(text = text + LINE_BREAK + line)

    /** 메시지 뒤에 이어진 빈 줄(날짜 구분선 앞 빈 줄, 파일 끝 줄바꿈)을 뗀다. */
    fun withoutTrailingBlankLines(): ChatMessage = copy(text = text.trimEnd())

    fun isWrittenBy(self: SpeakerName): Boolean = self.matches(speaker)

    fun hasText(): Boolean = text.isNotBlank()

    fun hasTextIn(candidates: Set<String>): Boolean = text in candidates

    /** 분석 이벤트로 바꾼다. 너무 긴 본문은 [AnalysisEvent.MAX_TEXT_LENGTH]자로 자른다. */
    fun toEvent(): AnalysisEvent = AnalysisEvent(occurredAt, text.take(AnalysisEvent.MAX_TEXT_LENGTH))

    companion object {
        private const val LINE_BREAK = "\n"
    }
}
