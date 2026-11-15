package com.santacut.presentation.controller

import com.santacut.application.command.AnalysisJobCommand
import com.santacut.application.facade.AnalysisFacade
import com.santacut.presentation.docs.StreamAnalysisApi
import com.santacut.presentation.docs.SubmitAnalysisApi
import com.santacut.presentation.request.SubmitAnalysisRequest
import com.santacut.presentation.response.AnalysisSubmissionResponse
import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.scheduling.TaskScheduler
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.time.Clock
import java.util.UUID

@RestController
@RequestMapping("/v1/analyses")
class AnalysisController(
    private val analysisFacade: AnalysisFacade,
    private val taskScheduler: TaskScheduler,
    private val clock: Clock,
) {
    @PostMapping
    @SubmitAnalysisApi
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun submit(
        @AuthenticationPrincipal @Parameter(hidden = true) participantId: UUID,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @Valid @RequestBody request: SubmitAnalysisRequest,
    ): AnalysisSubmissionResponse =
        AnalysisSubmissionResponse.from(analysisFacade.submit(request.toCommand(participantId, idempotencyKey)))

    @GetMapping("/{id}/events", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    @StreamAnalysisApi
    fun events(
        @AuthenticationPrincipal @Parameter(hidden = true) participantId: UUID,
        @PathVariable id: UUID,
    ): SseEmitter {
        val command = AnalysisJobCommand(participantId, id)
        // 스트림을 열기 전에 작업이 있고 본인 것인지 확인한다.
        analysisFacade.getStatus(command)
        return AnalysisEventStream(analysisFacade, taskScheduler, clock, command).open()
    }
}
