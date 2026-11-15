package com.santacut.domain.analysis.model

import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException

/**
 * 원본 파일에서 읽은 메시지 목록. 다른 참여자의 메시지도 들어 있으므로 본인 메시지만 이벤트로 바꿔 내보낸다.
 */
class ChatMessages(private val values: List<ChatMessage>) {
    fun toList(): List<ChatMessage> = values.toList()

    /** 메시지를 하나도 읽지 못했으면 지원하지 않는 형식이다. */
    fun verifyNotEmpty(): ChatMessages {
        if (values.isEmpty()) throw SantaCutException(ErrorCode.UNSUPPORTED_FILE_FORMAT)
        return this
    }

    /** 시각순으로 놓는다. 같은 시각 메시지는 원래 순서를 지킨다. */
    fun sortedByTime(): ChatMessages = ChatMessages(values.sortedBy { it.occurredAt })

    /**
     * 본인([selfSpeaker])이 쓴 메시지만 이벤트로 바꾼다. 다른 참여자의 메시지는 버리고 AI에도 보내지 않는다.
     * 본인 메시지가 하나도 없으면 [ErrorCode.SPEAKER_NOT_IDENTIFIED]이고, 빈 본문은 버린다.
     */
    fun eventsWrittenBy(selfSpeaker: String): AnalysisEvents {
        val self = SpeakerName.of(selfSpeaker)
        val own = values.filter { it.isWrittenBy(self) }
        if (own.isEmpty()) throw SantaCutException(ErrorCode.SPEAKER_NOT_IDENTIFIED)
        return AnalysisEvents(own.filter { it.hasText() }.map { it.toEvent() })
    }
}
