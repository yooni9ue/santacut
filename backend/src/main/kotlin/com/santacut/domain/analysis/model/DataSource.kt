package com.santacut.domain.analysis.model

/**
 * 정규화한 이벤트 데이터의 출처 플랫폼. 출처마다 원본 파일을 읽는 파서가 다르다.
 */
enum class DataSource {
    KAKAO {
        override fun readMessages(text: String, readInstagramThread: (String) -> InstagramThread): ChatMessages =
            KakaoTalkLines.from(text).toMessages()
    },
    INSTAGRAM {
        override fun readMessages(text: String, readInstagramThread: (String) -> InstagramThread): ChatMessages {
            InstagramMessages.verifyObjectCountIn(text)
            return readInstagramThread(text).toMessages()
        }
    },
    ;

    /**
     * 이 출처의 원본 텍스트를 메시지 목록으로 읽는다. 인스타그램 JSON은 [readInstagramThread]로 대화방 객체로 바꾼다.
     * 형식이 맞지 않으면 지원하지 않는 형식(UNSUPPORTED_FILE_FORMAT)이다.
     */
    abstract fun readMessages(text: String, readInstagramThread: (String) -> InstagramThread): ChatMessages
}
