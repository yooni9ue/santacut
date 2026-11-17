package com.santacut.presentation.analysis.controller

import com.santacut.application.analysis.command.AnalysisJobCommand
import com.santacut.application.analysis.command.LatestAnalysisCommand
import com.santacut.application.analysis.facade.AnalysisFacade
import com.santacut.application.analysis.facade.AnalysisStatusFacade
import com.santacut.presentation.analysis.docs.GetAnalysisApi
import com.santacut.presentation.analysis.docs.GetLatestAnalysisApi
import com.santacut.presentation.analysis.docs.SubmitAnalysisApi
import com.santacut.presentation.analysis.docs.UploadAnalysisApi
import com.santacut.presentation.analysis.request.SubmitAnalysisRequest
import com.santacut.presentation.analysis.request.UploadAnalysisRequest
import com.santacut.presentation.analysis.response.AnalysisStatusResponse
import com.santacut.presentation.analysis.response.AnalysisSubmissionResponse
import io.swagger.v3.oas.annotations.Parameter
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * 분석 제출과 진행 상황 조회. 클라이언트 IP는 운영 프로필에서 믿는 프록시(Caddy)의 X-Forwarded-For로 바꿔 둔 remoteAddr이다.
 */
@RestController
class AnalysisController(
    private val analysisFacade: AnalysisFacade,
    private val analysisStatusFacade: AnalysisStatusFacade,
) {
    @PostMapping("/v1/analyses")
    @SubmitAnalysisApi
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun submit(
        @AuthenticationPrincipal @Parameter(hidden = true) participantId: UUID,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @Valid @RequestBody request: SubmitAnalysisRequest,
        servletRequest: HttpServletRequest,
    ): AnalysisSubmissionResponse = AnalysisSubmissionResponse.from(
        analysisFacade.submit(request.toCommand(participantId, idempotencyKey, servletRequest.remoteAddr)),
    )

    @PostMapping("/v1/analyses/files")
    @UploadAnalysisApi
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun upload(
        @AuthenticationPrincipal @Parameter(hidden = true) participantId: UUID,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @Valid @ModelAttribute request: UploadAnalysisRequest,
        servletRequest: HttpServletRequest,
    ): AnalysisSubmissionResponse = AnalysisSubmissionResponse.from(
        analysisFacade.upload(request.toCommand(participantId, idempotencyKey, servletRequest.remoteAddr)),
    )

    @GetMapping("/v1/analyses/{id}")
    @GetAnalysisApi
    fun status(
        @AuthenticationPrincipal @Parameter(hidden = true) participantId: UUID,
        @PathVariable id: UUID,
    ): AnalysisStatusResponse =
        AnalysisStatusResponse.from(analysisStatusFacade.getStatus(AnalysisJobCommand(participantId, id)))

    @GetMapping("/v1/me/analysis")
    @GetLatestAnalysisApi
    fun latestStatus(@AuthenticationPrincipal @Parameter(hidden = true) participantId: UUID): AnalysisStatusResponse =
        AnalysisStatusResponse.from(analysisStatusFacade.getLatestStatus(LatestAnalysisCommand(participantId)))
}
