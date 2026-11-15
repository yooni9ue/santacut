package com.santacut.domain.analysis.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException

/**
 * 인스타그램 `message_1.json`의 `messages` 배열. 파일은 최신 메시지부터 적는다.
 */
class InstagramMessages @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(
    private val values: List<InstagramMessage?>,
) {
    /**
     * 본문이 있는 메시지만 시각순으로 바꾼다. 배열에 `null` 원소가 있거나 본문 있는 메시지가 하나도 없으면
     * 지원하지 않는 형식이다.
     */
    fun toMessages(): ChatMessages {
        if (values.contains(null)) throw SantaCutException(ErrorCode.UNSUPPORTED_FILE_FORMAT)
        return ChatMessages(values.mapNotNull { it?.toChatMessage() }).sortedByTime().verifyNotEmpty()
    }
}
