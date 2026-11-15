package com.santacut.presentation.analysis.controller

import com.santacut.application.analysis.command.AnalysisJobCommand
import com.santacut.application.analysis.command.LatestAnalysisCommand
import com.santacut.application.analysis.facade.AnalysisFacade
import com.santacut.presentation.analysis.docs.GetAnalysisApi
import com.santacut.presentation.analysis.docs.GetLatestAnalysisApi
import com.santacut.presentation.analysis.docs.SubmitAnalysisApi
import com.santacut.presentation.analysis.request.SubmitAnalysisRequest
import com.santacut.presentation.analysis.response.AnalysisStatusResponse
import com.santacut.presentation.analysis.response.AnalysisSubmissionResponse
import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class AnalysisController(
    private val analysisFacade: AnalysisFacade,
) {
    @PostMapping("/v1/analyses")
    @SubmitAnalysisApi
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun submit(
        @AuthenticationPrincipal @Parameter(hidden = true) participantId: UUID,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @Valid @RequestBody request: SubmitAnalysisRequest,
    ): AnalysisSubmissionResponse =
        AnalysisSubmissionResponse.from(analysisFacade.submit(request.toCommand(participantId, idempotencyKey)))

    @GetMapping("/v1/analyses/{id}")
    @GetAnalysisApi
    fun status(
        @AuthenticationPrincipal @Parameter(hidden = true) participantId: UUID,
        @PathVariable id: UUID,
    ): AnalysisStatusResponse =
        AnalysisStatusResponse.from(analysisFacade.getStatus(AnalysisJobCommand(participantId, id)))

    @GetMapping("/v1/me/analysis")
    @GetLatestAnalysisApi
    fun latestStatus(@AuthenticationPrincipal @Parameter(hidden = true) participantId: UUID): AnalysisStatusResponse =
        AnalysisStatusResponse.from(analysisFacade.getLatestStatus(LatestAnalysisCommand(participantId)))
}
