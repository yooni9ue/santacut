package com.santacut.presentation.analysis.controller

import com.santacut.application.analysis.command.OpenAnalysisStreamCommand
import com.santacut.application.analysis.facade.AnalysisFacade
import com.santacut.application.analysis.result.AnalysisStatusChangeResult
import com.santacut.presentation.analysis.docs.StreamAnalysisStatusApi
import com.santacut.presentation.analysis.response.AnalysisStatusEvent
import com.santacut.presentation.analysis.response.AnalysisStatusStream
import com.santacut.presentation.analysis.response.AnalysisStatusStreams
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.UUID

/**
 * 분석 작업 진행 상황을 SSE로 밀어 준다. 폴링(GET /v1/analyses/{id})을 대신할 수 있을 뿐 없애지 않는다.
 *
 * 열린 스트림은 이 인스턴스 메모리([AnalysisStatusStreams])에만 있다. 작업 상태가 바뀐 트랜잭션이 커밋되면
 * facade가 새 상태를 알리고, 이 controller가 그 참가자의 스트림으로 보낸다. 스트림마다 따로 DB를 다시 읽지 않는다.
 */
@RestController
class AnalysisStreamController(
    private val analysisFacade: AnalysisFacade,
) {
    private val streams = AnalysisStatusStreams()

    @GetMapping("/v1/analyses/{id}/events")
    @StreamAnalysisStatusApi
    fun events(
        @AuthenticationPrincipal @Parameter(hidden = true) participantId: UUID,
        @PathVariable id: UUID,
    ): SseEmitter {
        val command = OpenAnalysisStreamCommand(participantId, id, streams.size(), streams.hasStreamOf(participantId))
        val initial = AnalysisStatusEvent.from(analysisFacade.openStatusStream(command))
        val stream = AnalysisStatusStream(initial.jobId, SseEmitter(STREAM_TIMEOUT_MS))
        streams.open(participantId, stream, initial)
        return stream.emitter
    }

    /** 커밋된 상태 변경을 그 참가자의 스트림으로 보낸다. */
    @EventListener
    fun onStatusChange(change: AnalysisStatusChangeResult) {
        streams.publish(change.participantId, AnalysisStatusEvent.from(change.status))
    }

    /** 열린 스트림마다 SSE 주석을 보내 연결을 살려 둔다. 작업자와 같은 스케줄러 스레드라 작업이 길면 늦을 수 있다. */
    @Scheduled(fixedRate = HEARTBEAT_INTERVAL_MS)
    fun sendHeartbeats() {
        streams.heartbeat()
    }

    companion object {
        /** 서버가 스트림을 닫는 시간. 브라우저 EventSource는 닫히면 Last-Event-ID를 붙여 다시 연결한다. */
        private const val STREAM_TIMEOUT_MS = 300_000L
        private const val HEARTBEAT_INTERVAL_MS = 15_000L
    }
}
