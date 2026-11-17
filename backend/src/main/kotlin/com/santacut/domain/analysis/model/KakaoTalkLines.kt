package com.santacut.domain.analysis.model

import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException

/**
 * 카카오톡 TXT 내보내기의 모든 줄. PC, iOS, Android 형식을 읽는다.
 * 줄은 읽을 때 하나씩 만든다. 20MB 파일이 줄바꿈뿐이어도 줄 객체 수천만 개를 한꺼번에 들고 있지 않기 위해서다.
 */
class KakaoTalkLines(private val values: Sequence<KakaoTalkLine>) {
    /** 줄을 차례로 읽어 메시지 목록을 만든다. 메시지를 하나도 읽지 못하면 지원하지 않는 형식이다. */
    fun toMessages(): ChatMessages {
        val messages = mutableListOf<ChatMessage>()
        values.fold(KakaoTalkReading.START) { reading, line -> reading.read(line, messages::add) }
            .finish(messages::add)
        return ChatMessages(messages).verifyNotEmpty()
    }

    companion object {
        /**
         * 한 파일에서 읽는 줄 수의 상한. 메시지 줄은 시각·화자 접두어만 30바이트가 넘어 20MB 파일도 70만 줄을 넘기 어렵다.
         * 이보다 많으면 대화 내보내기가 아니라고 보고 읽기 전에 거른다.
         */
        const val MAX_LINES = 1_000_000

        private const val LINE_FEED = '\n'

        /** CRLF와 LF 줄바꿈을 모두 받는다. 줄이 [MAX_LINES]개보다 많으면 [ErrorCode.UNSUPPORTED_FILE_FORMAT]이다. */
        fun from(text: String): KakaoTalkLines {
            if (text.count { it == LINE_FEED } >= MAX_LINES) throw SantaCutException(ErrorCode.UNSUPPORTED_FILE_FORMAT)
            return KakaoTalkLines(text.lineSequence().map(::KakaoTalkLine))
        }
    }
}
