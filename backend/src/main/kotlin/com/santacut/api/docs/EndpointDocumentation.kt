package com.santacut.api.docs

import com.santacut.analysis.AnalysisInput
import com.santacut.analysis.AnalysisResultResponse
import com.santacut.analysis.AnalysisStatusResponse
import com.santacut.analysis.AnalysisSubmissionResponse
import com.santacut.core.CreateShareResponse
import com.santacut.core.CsrfResponse
import com.santacut.core.CurrentParticipantResponse
import com.santacut.core.MyResultResponse
import com.santacut.core.ParticipantRequest
import com.santacut.core.ParticipantResponse
import com.santacut.core.RankingResponse
import com.santacut.core.RegionResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement

private const val JSON = "application/json"
private const val SESSION_SECURITY = "sessionCookie"

data class ApiErrorResponse(
    @field:Schema(description = "Error time in UTC", format = "date-time", example = "2026-11-15T12:00:00Z") val timestamp: String,
    @field:Schema(description = "HTTP status code", example = "400") val status: Int,
    @field:Schema(description = "HTTP reason phrase", example = "Bad Request") val error: String,
    @field:Schema(description = "Request path", example = "/v1/analyses") val path: String
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    operationId = "getCsrfToken",
    summary = "Get a CSRF token",
    description = "Call before any POST or DELETE request. The response also sets the XSRF-TOKEN cookie; echo token in the header named by headerName.",
    tags = ["Session"],
    responses = [
        ApiResponse(responseCode = "200", description = "CSRF header name and token", headers = [Header(name = "Set-Cookie", description = "XSRF-TOKEN cookie required for subsequent write requests", schema = Schema(type = "string"))], content = [Content(mediaType = JSON, schema = Schema(implementation = CsrfResponse::class))]),
        ApiResponse(responseCode = "500", description = "Unexpected server error", content = [Content(mediaType = JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
    ]
)
annotation class GetCsrfApi

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    operationId = "listRegions",
    summary = "List supported regions",
    description = "Returns the Seoul districts currently supported for participant registration.",
    tags = ["Identity"],
    responses = [
        ApiResponse(responseCode = "200", description = "Supported region codes and display names", content = [Content(mediaType = JSON, array = ArraySchema(schema = Schema(implementation = RegionResponse::class)))]),
        ApiResponse(responseCode = "500", description = "Unexpected server error", content = [Content(mediaType = JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
    ]
)
annotation class ListRegionsApi

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    operationId = "getCurrentParticipant",
    summary = "Get the signed-in participant",
    description = "Returns the participant associated with the HttpOnly session cookie. Use this on page load to distinguish an active session from an anonymous browser.",
    tags = ["Identity"],
    security = [SecurityRequirement(name = SESSION_SECURITY)],
    responses = [
        ApiResponse(responseCode = "200", description = "Current participant profile", content = [Content(mediaType = JSON, schema = Schema(implementation = CurrentParticipantResponse::class))]),
        ApiResponse(responseCode = "401", description = "Session missing, invalid, or expired", content = [Content(mediaType = JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "404", description = "Session participant no longer exists", content = [Content(mediaType = JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "500", description = "Unexpected server error", content = [Content(mediaType = JSON, schema = Schema(implementation = ApiErrorResponse::class))])
    ]
)
annotation class GetCurrentParticipantApi

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    operationId = "registerParticipant",
    summary = "Register a participant",
    description = "Creates a participant and session. Nicknames are normalized with NFKC and compared case-insensitively. Fetch a CSRF token first. The session credential is returned only as an HttpOnly cookie, never in the JSON body.",
    tags = ["Identity"],
    parameters = [Parameter(name = "X-XSRF-TOKEN", `in` = ParameterIn.HEADER, required = true, description = "Token returned by GET /v1/csrf. Keep the XSRF-TOKEN cookie from that response.")],
    requestBody = RequestBody(required = true, content = [Content(mediaType = JSON, schema = Schema(implementation = ParticipantRequest::class))]),
    responses = [
        ApiResponse(responseCode = "201", description = "Participant created; session credential is set as an HttpOnly cookie", headers = [Header(name = "Set-Cookie", description = "Session cookie; sc_session locally or __Host-sc_session in production", schema = Schema(type = "string"))], content = [Content(mediaType = JSON, schema = Schema(implementation = ParticipantResponse::class))]),
        ApiResponse(responseCode = "400", description = "Invalid nickname, malformed request, or unsupported region", content = [Content(mediaType = JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "403", description = "CSRF token missing or invalid", content = [Content(mediaType = JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "409", description = "Nickname is already in use", content = [Content(mediaType = JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "500", description = "Unexpected server error", content = [Content(mediaType = JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
    ]
)
annotation class RegisterParticipantApi

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    operationId = "submitAnalysis",
    summary = "Submit an analysis",
    description = "Queues one analysis and returns immediately. Provide 20–2,000 normalized events; each text is 1–2,000 characters and total text is at most 200,000 characters. Event timestamps must be within the last 90 days and no more than 5 minutes in the future. A participant may submit at most 3 jobs per rolling 24 hours. Raw platform export files are not accepted. Reusing an idempotency key with the same body returns the original job; a different body conflicts. The AI evaluates care/empathy (30%), positivity/support (25%), respectful communication (25%), and cooperation/sharing (20%); the server validates evidence references and computes the weighted total.",
    tags = ["Analysis"],
    security = [SecurityRequirement(name = SESSION_SECURITY)],
    parameters = [
        Parameter(name = "Idempotency-Key", `in` = ParameterIn.HEADER, required = true, description = "16–100 ASCII letters, digits, underscore or hyphen; reuse only for the same request body."),
        Parameter(name = "X-XSRF-TOKEN", `in` = ParameterIn.HEADER, required = true, description = "Token returned by GET /v1/csrf.")
    ],
    requestBody = RequestBody(required = true, content = [Content(mediaType = JSON, schema = Schema(implementation = AnalysisInput::class))]),
    responses = [
        ApiResponse(responseCode = "202", description = "Job queued", content = [Content(mediaType = JSON, schema = Schema(implementation = AnalysisSubmissionResponse::class))]),
        ApiResponse(responseCode = "400", description = "Malformed request, validation failure, invalid idempotency key, or out-of-range event timestamp", content = [Content(mediaType = JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "401", description = "Session missing, invalid, or expired", content = [Content(mediaType = JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "403", description = "CSRF token missing or invalid", content = [Content(mediaType = JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "409", description = "Idempotency key reused with a different body, or season is frozen", content = [Content(mediaType = JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "429", description = "Participant daily submission limit reached", content = [Content(mediaType = JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "503", description = "AI model is not configured", content = [Content(mediaType = JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "500", description = "Unexpected server error", content = [Content(mediaType = JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
    ]
)
annotation class SubmitAnalysisApi

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    operationId = "streamAnalysisEvents",
    summary = "Stream analysis progress and result",
    description = "Server-Sent Events stream, polled every second for up to 150 seconds. Event names: analysis (job status), result (final score/rank/ETA), error (terminal failure). The client should reconnect only while the job is non-terminal. Only the job owner can subscribe.",
    tags = ["Analysis"],
    security = [SecurityRequirement(name = SESSION_SECURITY)],
    parameters = [Parameter(name = "id", `in` = ParameterIn.PATH, required = true, description = "UUID returned by POST /v1/analyses")],
    responses = [
        ApiResponse(responseCode = "200", description = "text/event-stream. Event names: analysis and error use AnalysisStatusResponse; result uses AnalysisResultResponse.", content = [Content(mediaType = "text/event-stream", schema = Schema(oneOf = [AnalysisStatusResponse::class, AnalysisResultResponse::class], format = "event-stream"))]),
        ApiResponse(responseCode = "401", description = "Session missing, invalid, or expired", content = [Content(mediaType = JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "404", description = "Job does not exist or belongs to another participant", content = [Content(mediaType = JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "500", description = "Unexpected server error", content = [Content(mediaType = JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
    ]
)
annotation class StreamAnalysisApi

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    operationId = "getMyResult",
    summary = "Get my latest result",
    description = "Returns the latest persisted assessment and the latest published ranking snapshot for the authenticated participant. score_hundredths is an integer divided by 100 for display; eta and computed_at are ISO-8601 UTC timestamps.",
    tags = ["Results"],
    security = [SecurityRequirement(name = SESSION_SECURITY)],
    responses = [
        ApiResponse(responseCode = "200", description = "Latest score, ranking, and simulated delivery time", content = [Content(mediaType = JSON, schema = Schema(implementation = MyResultResponse::class))]),
        ApiResponse(responseCode = "401", description = "Session missing, invalid, or expired", content = [Content(mediaType = JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "404", description = "No ranked result is available yet", content = [Content(mediaType = JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "500", description = "Unexpected server error", content = [Content(mediaType = JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
    ]
)
annotation class GetMyResultApi

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    operationId = "getRankings",
    summary = "Get the ranking page",
    description = "Returns at most 50 participants ordered by delivery sequence. offset is zero-based and limited to 0–10,000. score_hundredths is an integer number of hundredths (for example, 8250 means 82.50 points). eta and computed_at are ISO-8601 UTC timestamps; render ETA in Asia/Seoul. Delivery simulation starts at 00:00 Asia/Seoul on December 25, visits each registered district's representative coordinate in delivery order, assumes 100 km/h travel, 3 minutes per stop, and 6 active hours per day. This is a game simulation, not an actual delivery promise.",
    tags = ["Results"],
    security = [SecurityRequirement(name = SESSION_SECURITY)],
    parameters = [Parameter(name = "offset", `in` = ParameterIn.QUERY, required = false, description = "Zero-based row offset (default 0; range 0–10,000).", schema = Schema(type = "integer", defaultValue = "0", minimum = "0", maximum = "10000"))],
    responses = [
        ApiResponse(responseCode = "200", description = "Ranking rows for the requested page", content = [Content(mediaType = JSON, array = ArraySchema(schema = Schema(implementation = RankingResponse::class)))]),
        ApiResponse(responseCode = "400", description = "Offset is outside 0–10,000", content = [Content(mediaType = JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "401", description = "Session missing, invalid, or expired", content = [Content(mediaType = JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "500", description = "Unexpected server error", content = [Content(mediaType = JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
    ]
)
annotation class GetRankingsApi

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    operationId = "createShare",
    summary = "Create a share link for my latest result",
    description = "Creates or reuses a revocable public link containing a snapshot of the current result. The JSON response includes the HTML page URL and its 1200×630 PNG image URL. Fetch a CSRF token first.",
    tags = ["Sharing"],
    security = [SecurityRequirement(name = SESSION_SECURITY)],
    parameters = [Parameter(name = "X-XSRF-TOKEN", `in` = ParameterIn.HEADER, required = true, description = "Token returned by GET /v1/csrf.")],
    responses = [
        ApiResponse(responseCode = "200", description = "Share link created or reused", content = [Content(mediaType = JSON, schema = Schema(implementation = CreateShareResponse::class))]),
        ApiResponse(responseCode = "401", description = "Session missing, invalid, or expired", content = [Content(mediaType = JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "403", description = "CSRF token missing or invalid", content = [Content(mediaType = JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "409", description = "No ranked result is available to share", content = [Content(mediaType = JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "500", description = "Unexpected server error", content = [Content(mediaType = JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
    ]
)
annotation class CreateShareApi

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    operationId = "revokeShare",
    summary = "Revoke my share link",
    description = "Deletes the share token. Its public HTML and PNG URLs then return 404. Fetch a CSRF token first.",
    tags = ["Sharing"],
    security = [SecurityRequirement(name = SESSION_SECURITY)],
    parameters = [
        Parameter(name = "token", `in` = ParameterIn.PATH, required = true, description = "43-character base64url share token."),
        Parameter(name = "X-XSRF-TOKEN", `in` = ParameterIn.HEADER, required = true, description = "Token returned by GET /v1/csrf.")
    ],
    responses = [
        ApiResponse(responseCode = "204", description = "Share link revoked"),
        ApiResponse(responseCode = "401", description = "Session missing, invalid, or expired", content = [Content(mediaType = JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "403", description = "CSRF token missing or invalid", content = [Content(mediaType = JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "404", description = "Share token does not exist or belongs to another participant", content = [Content(mediaType = JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "500", description = "Unexpected server error", content = [Content(mediaType = JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
    ]
)
annotation class RevokeShareApi

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    operationId = "getPublicSharePage",
    summary = "View a public share page",
    description = "Returns a server-rendered HTML page with Open Graph metadata for social previews. Contains the immutable result snapshot captured when the share was created.",
    tags = ["Public sharing"],
    parameters = [Parameter(name = "token", `in` = ParameterIn.PATH, required = true, description = "43-character base64url share token.")],
    responses = [
        ApiResponse(responseCode = "200", description = "HTML page with Open Graph metadata", content = [Content(mediaType = "text/html", schema = Schema(type = "string"))]),
        ApiResponse(responseCode = "404", description = "Share token is invalid, revoked, or unknown", content = [Content(mediaType = JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "500", description = "Unexpected server error", content = [Content(mediaType = JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
    ]
)
annotation class PublicSharePageApi

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    operationId = "getPublicShareImage",
    summary = "Get the public share image",
    description = "Returns a generated 1200×630 PNG card for social sharing. The image contains the saved share snapshot, not a live ranking query.",
    tags = ["Public sharing"],
    parameters = [Parameter(name = "token", `in` = ParameterIn.PATH, required = true, description = "43-character base64url share token.")],
    responses = [
        ApiResponse(responseCode = "200", description = "PNG result card", content = [Content(mediaType = "image/png", schema = Schema(type = "string", format = "binary"))]),
        ApiResponse(responseCode = "404", description = "Share token is invalid, revoked, or unknown", content = [Content(mediaType = JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "500", description = "Unexpected server error", content = [Content(mediaType = JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
    ]
)
annotation class PublicShareImageApi
