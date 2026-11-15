package com.santacut.domain.analysis.model

/**
 * 카카오톡 TXT 내보내기의 모든 줄. PC, iOS, Android 형식을 읽는다.
 */
class KakaoTalkLines(private val values: List<KakaoTalkLine>) {
    /** 줄을 차례로 읽어 메시지 목록을 만든다. 메시지를 하나도 읽지 못하면 지원하지 않는 형식이다. */
    fun toMessages(): ChatMessages {
        val messages = mutableListOf<ChatMessage>()
        values.fold(KakaoTalkReading.START) { reading, line -> reading.read(line, messages::add) }
            .finish(messages::add)
        return ChatMessages(messages).verifyNotEmpty()
    }

    companion object {
        /** CRLF와 LF 줄바꿈을 모두 받는다. */
        fun from(text: String): KakaoTalkLines = KakaoTalkLines(text.lines().map(::KakaoTalkLine))
    }
}
