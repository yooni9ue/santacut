package com.santacut.presentation.global.advice

import com.santacut.application.share.facade.ShareFacade
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
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.multipart.MultipartException
import org.springframework.web.multipart.support.MissingServletRequestPartException
import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * 비즈니스 오류와 요청 형식 오류를 [ApiErrorResponse] JSON으로 바꾼다. 모든 오류 본문에 code가 붙는다.
 * HTML, PNG 엔드포인트에서도 오류 본문은 JSON으로 보낸다. 다만 브라우저(Accept에 text/html)가 연 공유 페이지가
 * 없거나 취소됐으면 JSON 대신 "더 이상 볼 수 없는 결과" HTML 페이지를 404로 보낸다(기획 §5).
 * 응답 문서는 엔드포인트별 애노테이션에 있으므로 Swagger 일반 응답으로는 노출하지 않는다.
 */
@Hidden
@RestControllerAdvice
class ApiExceptionHandler(
    private val clock: Clock,
    private val shareFacade: ShareFacade,
) {
    @ExceptionHandler(SantaCutException::class)
    fun handleSantaCutException(
        exception: SantaCutException,
        request: HttpServletRequest,
    ): ResponseEntity<*> {
        if (isInactiveSharePageRequest(exception, request)) return inactiveSharePage()
        return respond(exception.errorCode, request, exception.retryAt)
    }

    /** 본문 검증·파싱 실패, 필수 헤더·쿼리 누락, 경로·쿼리 값 형식 오류. */
    @ExceptionHandler(
        MethodArgumentNotValidException::class,
        HandlerMethodValidationException::class,
        HttpMessageNotReadableException::class,
        ServletRequestBindingException::class,
        TypeMismatchException::class,
        MissingServletRequestPartException::class,
        MultipartException::class,
    )
    fun handleInvalidRequest(request: HttpServletRequest): ResponseEntity<ApiErrorResponse> =
        respond(ErrorCode.INVALID_REQUEST, request, null)

    /** multipart 파일이 `spring.servlet.multipart.max-file-size`(20MB)를 넘었다. */
    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleFileTooLarge(request: HttpServletRequest): ResponseEntity<ApiErrorResponse> =
        respond(ErrorCode.FILE_TOO_LARGE, request, null)

    /** 보안 필터가 인증 실패를 이 처리기로 넘긴다. 세션이 없거나 만료된 요청이다. */
    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthentication(request: HttpServletRequest): ResponseEntity<ApiErrorResponse> =
        respond(ErrorCode.NOT_AUTHENTICATED, request, null)

    private fun isInactiveSharePageRequest(exception: SantaCutException, request: HttpServletRequest): Boolean =
        exception.errorCode == ErrorCode.SHARE_NOT_FOUND &&
            request.requestURI.startsWith(SHARE_PAGE_PATH_PREFIX) &&
            request.getHeader(HttpHeaders.ACCEPT)?.contains(MediaType.TEXT_HTML_VALUE) == true

    private fun inactiveSharePage(): ResponseEntity<String> = ResponseEntity.status(HttpStatus.NOT_FOUND)
        .contentType(HTML_UTF8)
        .header(HttpHeaders.CACHE_CONTROL, NO_STORE)
        .body(shareFacade.getInactivePage().html)

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
        private const val SHARE_PAGE_PATH_PREFIX = "/s/"
        private const val NO_STORE = "no-store"
        private val HTML_UTF8 = MediaType(MediaType.TEXT_HTML, Charsets.UTF_8)
    }
}
