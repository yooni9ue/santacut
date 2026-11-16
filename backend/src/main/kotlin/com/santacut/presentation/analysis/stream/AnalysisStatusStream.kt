package com.santacut.presentation.analysis.stream

import org.springframework.http.MediaType
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException

/**
 * 참가자 한 명이 연 진행 상황 스트림. 작업 하나의 `status` 이벤트만 보내고, 끝난 단계를 보내면 닫는다.
 * 클라이언트가 끊었거나 시간이 다 됐거나 오류가 나면 [onFinish]로 등록한 정리 작업을 한 번 부른다.
 *
 * @property jobId 이 스트림이 지켜보는 분석 작업
 */
class AnalysisStatusStream(
    val jobId: String,
    val emitter: SseEmitter,
) {
    private var lastStatus: String? = null
    private var finishAction: () -> Unit = {}
    private var finished = false

    fun isOpen(): Boolean = !finished

    /** 정리 작업을 등록하고 SSE 연결의 완료·시간 초과·오류에 잇는다. */
    fun onFinish(action: () -> Unit) {
        finishAction = action
        emitter.onCompletion(::finish)
        emitter.onTimeout(::close)
        emitter.onError { finish() }
    }

    /** 이 작업의 이벤트이고 직전에 보낸 단계와 다를 때만 보낸다. 끝난 단계를 보내면 닫는다. */
    @Synchronized
    fun send(event: AnalysisStatusEvent) {
        if (finished || event.jobId != jobId || event.body.status == lastStatus) return
        val sent = trySend(statusEvent(event))
        lastStatus = event.body.status
        if (sent && event.terminal) close()
    }

    /** 연결을 살려 두는 SSE 주석 한 줄. 프록시가 쉬는 연결을 끊지 않게 한다. */
    @Synchronized
    fun heartbeat() {
        if (!finished) trySend(SseEmitter.event().comment(HEARTBEAT_COMMENT))
    }

    /** 스트림을 정상 종료한다. 같은 참가자가 새 스트림을 열었거나 시간이 다 됐거나 끝난 단계를 보냈을 때다. */
    @Synchronized
    fun close() {
        if (finished) return
        emitter.complete()
        finish()
    }

    private fun statusEvent(event: AnalysisStatusEvent): SseEmitter.SseEventBuilder =
        SseEmitter.event().name(EVENT_NAME).id(event.eventId.toString()).data(event.body, MediaType.APPLICATION_JSON)

    /** 보내지 못하면(클라이언트가 끊음, 이미 끝난 연결) 스트림을 오류로 끝내고 정리한다. */
    private fun trySend(event: SseEmitter.SseEventBuilder): Boolean =
        try {
            emitter.send(event)
            true
        } catch (exception: IOException) {
            failWith(exception)
        } catch (exception: IllegalStateException) {
            failWith(exception)
        }

    private fun failWith(exception: Exception): Boolean {
        emitter.completeWithError(exception)
        finish()
        return false
    }

    private fun finish() {
        if (finished) return
        finished = true
        finishAction()
    }

    companion object {
        const val EVENT_NAME = "status"
        private const val HEARTBEAT_COMMENT = "heartbeat"
    }
}
