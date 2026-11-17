package com.santacut.presentation.global.security

import com.santacut.presentation.global.advice.RequestBodyTooLargeException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.servlet.HandlerExceptionResolver

/**
 * 파일 업로드(multipart)가 아닌 요청의 본문 크기를 Content-Length로 막는다. 넘으면 본문을 읽지 않고 REQUEST_TOO_LARGE(413)다.
 * multipart 상한은 `spring.servlet.multipart`가 맡고, Content-Length가 없는 요청은 앞단 Caddy의 `request_body` 상한이 막는다.
 */
class RequestBodyLimitFilter(
    private val maxBodyBytes: Long,
    private val handlerExceptionResolver: HandlerExceptionResolver,
) : OncePerRequestFilter() {
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        if (isTooLarge(request)) {
            handlerExceptionResolver.resolveException(request, response, null, RequestBodyTooLargeException())
            return
        }
        chain.doFilter(request, response)
    }

    private fun isTooLarge(request: HttpServletRequest): Boolean =
        !isMultipart(request) && request.contentLengthLong > maxBodyBytes

    private fun isMultipart(request: HttpServletRequest): Boolean =
        request.contentType?.startsWith(MediaType.MULTIPART_FORM_DATA_VALUE, ignoreCase = true) == true
}
