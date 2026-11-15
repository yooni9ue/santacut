package com.santacut.presentation.global.advice

import com.santacut.domain.global.exception.SantaCutException
import io.swagger.v3.oas.annotations.Hidden
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Clock

/**
 * 비즈니스 오류를 [ApiErrorResponse] JSON으로 바꾼다.
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
    ): ResponseEntity<ApiErrorResponse> {
        val status = HttpStatus.valueOf(exception.errorCode.status)
        val body = ApiErrorResponse(
            timestamp = clock.instant().toString(),
            status = status.value(),
            error = status.reasonPhrase,
            path = request.requestURI,
            code = exception.errorCode.name,
            message = exception.errorCode.message,
        )
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body)
    }
}
