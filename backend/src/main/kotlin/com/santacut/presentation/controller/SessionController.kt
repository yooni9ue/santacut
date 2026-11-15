package com.santacut.presentation.controller

import com.santacut.presentation.docs.GetCsrfApi
import com.santacut.presentation.response.CsrfResponse
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class SessionController {
    @GetMapping("/v1/csrf")
    @GetCsrfApi
    fun csrf(@Parameter(hidden = true) csrfToken: CsrfToken): CsrfResponse = CsrfResponse.from(csrfToken)
}
