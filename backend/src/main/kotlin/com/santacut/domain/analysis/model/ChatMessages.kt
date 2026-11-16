package com.santacut.domain.analysis.model

import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException

/**
 * 원본 파일에서 읽은 메시지 목록. 본인과 다른 참여자의 메시지를 모두 이벤트로 바꾸되, 참여자 이름은 버리고
 * 본인은 표시 없이, 다른 참여자는 `상대`(한 명)나 처음 나온 순서의 `상대1`, `상대2`(여럿)로 바꾼다.
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
     * 본인([selfSpeaker])과 다른 참여자의 메시지를 이벤트로 바꾼다. 다른 참여자는 화자 표시만 남긴다.
     * 본인 메시지가 하나도 없으면 [ErrorCode.SPEAKER_NOT_IDENTIFIED]이고, 빈 본문은 버린다.
     */
    fun eventsFor(selfSpeaker: String): AnalysisEvents {
        val self = SpeakerName.of(selfSpeaker)
        if (values.none { it.isWrittenBy(self) }) throw SantaCutException(ErrorCode.SPEAKER_NOT_IDENTIFIED)
        val partners = values.filterNot { it.isWrittenBy(self) }.map { it.speaker }.distinct()
        return AnalysisEvents(values.filter { it.hasText() }.map { it.toEvent(partnerLabel(it, self, partners)) })
    }

    /** 본인이면 null, 다른 참여자가 한 명이면 `상대`, 여럿이면 처음 나온 순서대로 `상대1`, `상대2`다. */
    private fun partnerLabel(message: ChatMessage, self: SpeakerName, partners: List<String>): String? = when {
        message.isWrittenBy(self) -> null
        partners.size == 1 -> PARTNER
        else -> PARTNER + (partners.indexOf(message.speaker) + 1)
    }

    companion object {
        private const val PARTNER = "상대"
    }
}
