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

    companion object {
        /**
         * 한 파일에서 읽는 JSON 객체 수의 상한. 실제 메시지 객체는 보낸 사람·시각·본문만으로도 수십 바이트라 20MB 파일도 이보다 적다.
         * `[{},{},...]`처럼 빈 객체만 채운 파일은 객체 수백만 개로 메모리를 다 쓰므로 파싱 전에 거른다.
         */
        const val MAX_OBJECTS = 1_000_000

        private const val OBJECT_START = '{'

        /** 여는 중괄호 수(문자열 안의 것 포함)로 객체 수의 위쪽 어림을 잡아 [MAX_OBJECTS]를 넘으면 [ErrorCode.UNSUPPORTED_FILE_FORMAT]이다. */
        fun verifyObjectCountIn(text: String) {
            if (text.count { it == OBJECT_START } > MAX_OBJECTS) throw SantaCutException(ErrorCode.UNSUPPORTED_FILE_FORMAT)
        }
    }
}
