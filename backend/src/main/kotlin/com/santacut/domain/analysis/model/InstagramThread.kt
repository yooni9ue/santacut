package com.santacut.domain.analysis.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException

/**
 * 인스타그램 내보내기의 대화방 파일(`messages/inbox/<대화방>/message_1.json`).
 * 파일에 소유자 표시가 없으므로 참가자가 본인 이름을 따로 고른다. 참여자 목록 등 다른 필드는 읽지 않는다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class InstagramThread(
    @JsonProperty("messages") private val messages: InstagramMessages?,
) {
    /** `messages` 배열이 없으면 지원하지 않는 형식이다. */
    fun toMessages(): ChatMessages =
        (messages ?: throw SantaCutException(ErrorCode.UNSUPPORTED_FILE_FORMAT)).toMessages()
}
