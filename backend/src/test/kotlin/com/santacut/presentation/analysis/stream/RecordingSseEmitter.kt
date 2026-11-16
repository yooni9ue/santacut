package com.santacut.presentation.analysis.stream

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException

/** 보낸 SSE 이벤트를 글자 그대로 모아 두는 시험용 emitter. [failing]이면 보내기마다 연결이 끊긴 것처럼 실패한다. */
class RecordingSseEmitter(
    private val failing: Boolean = false,
) : SseEmitter(TIMEOUT_MS) {
    val sent = mutableListOf<String>()

    override fun send(builder: SseEventBuilder) {
        if (failing) throw IOException("연결 끊김")
        sent.add(builder.build().joinToString("") { part -> part.data.toString() })
    }

    companion object {
        private const val TIMEOUT_MS = 1_000L
    }
}
