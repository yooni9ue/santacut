package com.santacut.domain.analysis.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

/**
 * 인스타그램 `message_1.json`의 메시지 하나. 사진·공유 같은 다른 필드는 읽지 않는다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class InstagramMessage(
    @JsonProperty("sender_name") private val senderName: String?,
    @JsonProperty("timestamp_ms") private val timestampMs: Long?,
    @JsonProperty("content") private val content: String?,
) {
    /** 보낸 사람, 시각, 본문이 모두 있을 때만 메시지다. 깨진 글자는 되살린다. */
    fun toChatMessage(): ChatMessage? {
        if (senderName == null || timestampMs == null || content.isNullOrBlank()) return null
        return ChatMessage(MojibakeText.repair(senderName), Instant.ofEpochMilli(timestampMs), MojibakeText.repair(content))
    }
}
