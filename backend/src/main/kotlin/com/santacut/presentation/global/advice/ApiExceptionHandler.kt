package com.santacut.presentation.global.advice

import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import io.swagger.v3.oas.annotations.Hidden
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.TypeMismatchException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.ServletRequestBindingException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * 비즈니스 오류와 요청 형식 오류를 [ApiErrorResponse] JSON으로 바꾼다. 모든 오류 본문에 code가 붙는다.
 * HTML, PNG 엔드포인트에서도 오류 본문은 JSON으로 보낸다.
 * 응답 문서는 엔드포인트별 애노테이션에 있으므로 Swagger 일반 응답으로는 노출하지 않는다.
 */
@Hidden
@RestControllerAdvice
class ApiExceptionHandler(
    private val clock: Clock,
) {
    @ExceptionHandler(SantaCutException::class)
    fun handleSantaCutException(
        exception: SantaCutException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiErrorResponse> = respond(exception.errorCode, request, exception.retryAt)

    /** 본문 검증·파싱 실패, 필수 헤더·쿼리 누락, 경로·쿼리 값 형식 오류. */
    @ExceptionHandler(
        MethodArgumentNotValidException::class,
        HandlerMethodValidationException::class,
        HttpMessageNotReadableException::class,
        ServletRequestBindingException::class,
        TypeMismatchException::class,
    )
    fun handleInvalidRequest(request: HttpServletRequest): ResponseEntity<ApiErrorResponse> =
        respond(ErrorCode.INVALID_REQUEST, request, null)

    /** 보안 필터가 인증 실패를 이 처리기로 넘긴다. 세션이 없거나 만료된 요청이다. */
    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthentication(request: HttpServletRequest): ResponseEntity<ApiErrorResponse> =
        respond(ErrorCode.NOT_AUTHENTICATED, request, null)

    private fun respond(errorCode: ErrorCode, request: HttpServletRequest, retryAt: Instant?): ResponseEntity<ApiErrorResponse> {
        val now = clock.instant()
        val status = HttpStatus.valueOf(errorCode.status)
        val body = ApiErrorResponse(
            now.toString(), status.value(), status.reasonPhrase, request.requestURI,
            errorCode.name, errorCode.message, retryAt?.toString(),
        )
        val builder = ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON)
        retryAt?.let { builder.header(HttpHeaders.RETRY_AFTER, retryAfterSeconds(now, it).toString()) }
        return builder.body(body)
    }

    /** Retry-After 헤더 값(초). 이미 지난 시각이어도 최소 1초를 준다. */
    private fun retryAfterSeconds(now: Instant, retryAt: Instant): Long {
        val duration = Duration.between(now, retryAt)
        val seconds = duration.seconds + if (duration.nano > 0) 1 else 0
        return seconds.coerceAtLeast(MIN_RETRY_AFTER_SECONDS)
    }

    companion object {
        private const val MIN_RETRY_AFTER_SECONDS = 1L
    }
}
