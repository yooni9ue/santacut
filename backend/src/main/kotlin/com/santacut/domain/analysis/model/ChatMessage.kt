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
    /**
     * 메시지 줄 다음에 이어진 줄을 본문 새 줄로 붙인다. 이벤트로 바꿀 때 [AnalysisEvent.MAX_TEXT_LENGTH]자에서 자르므로
     * 본문도 그 길이까지만 모은다. 이어진 줄이 수만 개여도 한 번 붙일 때 복사하는 길이가 이 상한을 넘지 않는다.
     */
    fun continuedWith(line: String): ChatMessage {
        if (text.length >= AnalysisEvent.MAX_TEXT_LENGTH) return this
        val joined = StringBuilder(text).append(LINE_BREAK).append(line, 0, minOf(line.length, AnalysisEvent.MAX_TEXT_LENGTH))
        return copy(text = joined.take(AnalysisEvent.MAX_TEXT_LENGTH).toString())
    }

    /** 메시지 뒤에 이어진 빈 줄(날짜 구분선 앞 빈 줄, 파일 끝 줄바꿈)을 뗀다. */
    fun withoutTrailingBlankLines(): ChatMessage = copy(text = text.trimEnd())

    fun isWrittenBy(self: SpeakerName): Boolean = self.matches(speaker)

    fun hasText(): Boolean = text.isNotBlank()

    fun hasTextIn(candidates: Set<String>): Boolean = text in candidates

    /** 분석 이벤트로 바꾼다. 너무 긴 본문은 [AnalysisEvent.MAX_TEXT_LENGTH]자로 자른다. [partner]는 상대 화자 표시, 본인이면 null이다. */
    fun toEvent(partner: String?): AnalysisEvent = AnalysisEvent(occurredAt, text.take(AnalysisEvent.MAX_TEXT_LENGTH), partner)

    companion object {
        private const val LINE_BREAK = "\n"
    }
}
