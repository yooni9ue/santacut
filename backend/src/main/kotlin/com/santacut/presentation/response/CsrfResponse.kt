package com.santacut.presentation.response

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.security.web.csrf.CsrfToken

data class CsrfResponse(
    @field:Schema(description = "보낼 요청 헤더 이름. 보통 X-XSRF-TOKEN입니다.", example = "X-XSRF-TOKEN")
    val headerName: String,
    @field:Schema(description = "보낼 CSRF 토큰 값. headerName 헤더에 그대로 보내고 XSRF-TOKEN 쿠키도 유지합니다.", example = "e9d3...f61a")
    val token: String,
) {
    companion object {
        fun from(csrfToken: CsrfToken): CsrfResponse = CsrfResponse(csrfToken.headerName, csrfToken.token)
    }
}
