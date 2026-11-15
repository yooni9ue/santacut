package com.santacut.presentation.analysis.controller

import com.santacut.application.analysis.command.AnalysisJobCommand
import com.santacut.application.analysis.facade.AnalysisFacade
import com.santacut.application.analysis.result.AnalysisStatusResult
import com.santacut.presentation.analysis.response.AnalysisResultResponse
import com.santacut.presentation.analysis.response.AnalysisStatusResponse
import org.slf4j.LoggerFactory
import org.springframework.scheduling.TaskScheduler
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.time.Clock
import java.time.Duration
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * 분석 작업 하나의 SSE 스트림. 작업 상태를 1초마다 확인해 보내고, 끝나면 결과나 오류 이벤트를 보내고 닫는다.
 * 요청마다 새로 만드는 객체이며 Spring 빈이 아니다. [AnalysisController]는 스트림을 만들어 열기만 한다.
 */
class AnalysisEventStream(
    private val analysisFacade: AnalysisFacade,
    private val taskScheduler: TaskScheduler,
    private val clock: Clock,
    private val command: AnalysisJobCommand,
) {
    private val emitter = SseEmitter(STREAM_TIMEOUT_MILLIS)
    private val polls = AtomicInteger()
    private val finished = AtomicBoolean(false)
    private val scheduled = AtomicReference<ScheduledFuture<*>?>()

    fun open(): SseEmitter {
        val firstPoll = clock.instant().plus(POLL_INTERVAL)
        scheduled.set(taskScheduler.scheduleAtFixedRate({ poll() }, firstPoll, POLL_INTERVAL))
        emitter.onCompletion { release() }
        emitter.onTimeout { release() }
        emitter.onError { release() }
        return emitter
    }

    private fun poll() {
        if (finished.get()) return
        try {
            publish(analysisFacade.getStatus(command))
        } catch (exception: Exception) {
            logPollFailure(exception)
            abort()
        }
    }

    /**
     * 클라이언트가 연결을 끊어 보내기에 실패한 것은 흔한 일이라 디버그로만 남기고,
     * 조회 실패(작업이 사라짐 등)나 예상하지 못한 장애는 경고로 남긴다. 제출 원문은 로그에 남기지 않는다.
     */
    private fun logPollFailure(exception: Exception) {
        if (exception is IOException) {
            logger.debug("분석 스트림 전송 실패로 닫음: 작업 {}, {}", command.jobId, exception.javaClass.simpleName)
            return
        }
        logger.warn(
            "분석 스트림 폴링 실패: 작업 {}, {}: {}",
            command.jobId, exception.javaClass.simpleName, exception.message,
        )
    }

    private fun publish(status: AnalysisStatusResult) {
        val statusResponse = AnalysisStatusResponse.from(status)
        emitter.send(SseEmitter.event().name(ANALYSIS_EVENT).data(statusResponse))
        when {
            status.succeeded -> finish(RESULT_EVENT, AnalysisResultResponse.from(analysisFacade.getOutcome(command)))
            status.failed -> finish(ERROR_EVENT, statusResponse)
            polls.incrementAndGet() >= MAX_POLLS -> stop()
        }
    }

    private fun finish(eventName: String, payload: Any) {
        emitter.send(SseEmitter.event().name(eventName).data(payload))
        stop()
    }

    private fun stop() {
        if (finished.compareAndSet(false, true)) emitter.complete()
        scheduled.get()?.cancel(false)
    }

    private fun abort() {
        finished.set(true)
        emitter.completeWithError(IllegalStateException(STREAM_ABORTED_MESSAGE))
        scheduled.get()?.cancel(false)
    }

    private fun release() {
        finished.set(true)
        scheduled.get()?.cancel(false)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AnalysisEventStream::class.java)
        private const val STREAM_TIMEOUT_MILLIS = 180_000L
        private const val MAX_POLLS = 150
        private val POLL_INTERVAL: Duration = Duration.ofSeconds(1)
        private const val ANALYSIS_EVENT = "analysis"
        private const val RESULT_EVENT = "result"
        private const val ERROR_EVENT = "error"
        private const val STREAM_ABORTED_MESSAGE = "분석 이벤트 스트림을 닫았습니다."
    }
}
