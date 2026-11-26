package com.santacut.presentation.global.security

import com.santacut.application.session.facade.SessionFacade
import com.santacut.presentation.global.config.CorsProperties
import com.santacut.presentation.global.config.PromptAdminProperties
import com.santacut.presentation.global.config.RequestBodyProperties
import com.santacut.presentation.global.docs.ApiDocs
import com.santacut.presentation.global.config.SessionCookieProperties
import jakarta.servlet.DispatcherType
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.authorization.AuthorizationManager
import org.springframework.security.core.AuthenticationException
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy
import org.springframework.security.web.access.intercept.RequestAuthorizationContext
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.csrf.CsrfFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.servlet.HandlerExceptionResolver

/**
 * 쿠키 세션과 CSRF 쿠키로 동작하는 무상태 보안 설정.
 *
 * 세션 필터는 HTTP 진입점이라 controller처럼 [SessionFacade]로 세션을 확인한다.
 * 인증 실패는 [HandlerExceptionResolver]로 넘겨 다른 오류와 같은 JSON 본문(code NOT_AUTHENTICATED)으로 응답한다.
 * 프론트엔드(www.santacut.me)와 API(api.santacut.me)는 출처가 달라 CORS로 허용한 출처에만 쿠키를 실은 요청을 연다.
 */
@Configuration
class SecurityConfiguration(
    private val sessionFacade: SessionFacade,
    private val sessionCookieProperties: SessionCookieProperties,
    private val corsProperties: CorsProperties,
    private val promptAdminProperties: PromptAdminProperties,
    private val requestBodyProperties: RequestBodyProperties,
    @param:Qualifier(HANDLER_EXCEPTION_RESOLVER) private val handlerExceptionResolver: HandlerExceptionResolver,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.sessionManagement {
            it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .sessionAuthenticationStrategy(NullAuthenticatedSessionStrategy())
        }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .csrf { it.csrfTokenRepository(csrfTokenRepository()) }
            .authorizeHttpRequests(::authorizeRequests)
            .addFilterBefore(requestBodyLimitFilter(), CsrfFilter::class.java)
            .addFilterBefore(sessionFilter(), AnonymousAuthenticationFilter::class.java)
            .exceptionHandling { it.authenticationEntryPoint(::rejectUnauthenticated) }
            .headers { it.contentSecurityPolicy { policy -> policy.policyDirectives(CONTENT_SECURITY_POLICY) } }
        return http.build()
    }

    /**
     * 공개 경로는 열어 두고 나머지는 로그인 세션을 요구한다. 공개 경로도 쓰기 요청은 CSRF 토큰이 있어야 한다.
     * 비동기 디스패치(SSE 스트림이 끝날 때 다시 들어오는 요청)는 처음 요청에서 이미 인증했으므로 다시 검사하지 않는다.
     * 관리 API는 세션 대신 `X-Admin-Token` 헤더로만 연다. 토큰을 설정하지 않았으면 컨트롤러가 없어 404다.
     */
    private fun authorizeRequests(
        registry: AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry,
    ) {
        registry.dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
            .requestMatchers(ADMIN_PATHS).access(adminTokenAuthorization())
            .requestMatchers(*PUBLIC_PATHS).permitAll()
            .anyRequest().authenticated()
    }

    /**
     * 토큰을 설정하지 않았으면 관리 컨트롤러가 없으므로 통과시켜 404가 나게 한다. 설정했으면 헤더 값이 같아야 한다.
     * 그래서 `/v1/admin/` 아래 컨트롤러는 모두 `PromptAdminCondition`으로만 올린다.
     */
    private fun adminTokenAuthorization(): AuthorizationManager<RequestAuthorizationContext> =
        AuthorizationManager { _, context ->
            val presented = context.request.getHeader(ApiDocs.ADMIN_TOKEN_HEADER)
            AuthorizationDecision(promptAdminProperties.token.isBlank() || promptAdminProperties.accepts(presented))
        }

    /** HTTPS(Secure)에서는 하위 도메인이 덮어쓰지 못하게 CSRF 쿠키에도 `__Host-` 이름을 쓴다. */
    private fun csrfTokenRepository(): CookieCsrfTokenRepository = CookieCsrfTokenRepository().apply {
        setCookieName(if (sessionCookieProperties.sessionSecure) SECURE_CSRF_COOKIE else CSRF_COOKIE)
        setCookieCustomizer { it.secure(sessionCookieProperties.sessionSecure).sameSite(sessionCookieProperties.sessionSameSite).path(ROOT_PATH) }
    }

    /** 허용 출처에서 오는 쿠키 실은 요청을 연다. CSRF 헤더를 받고 Retry-After를 읽을 수 있게 한다. */
    private fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOrigins = corsProperties.allowedOrigins
            allowedMethods = CORS_METHODS
            allowedHeaders = CORS_HEADERS
            exposedHeaders = CORS_EXPOSED_HEADERS
            allowCredentials = true
            maxAge = CORS_MAX_AGE_SECONDS
        }
        return UrlBasedCorsConfigurationSource().apply { registerCorsConfiguration(ALL_PATHS, configuration) }
    }

    private fun rejectUnauthenticated(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException,
    ) {
        handlerExceptionResolver.resolveException(request, response, null, exception)
    }

    private fun requestBodyLimitFilter(): RequestBodyLimitFilter =
        RequestBodyLimitFilter(requestBodyProperties.maxSize.toBytes(), handlerExceptionResolver)

    private fun sessionFilter(): SessionFilter =
        SessionFilter(sessionFacade, SessionCookie.name(sessionCookieProperties.sessionSecure))

    companion object {
        private const val HANDLER_EXCEPTION_RESOLVER = "handlerExceptionResolver"
        private const val ROOT_PATH = "/"
        private const val ADMIN_PATHS = "/v1/admin/**"
        private const val CSRF_COOKIE = "XSRF-TOKEN"
        private const val SECURE_CSRF_COOKIE = "__Host-XSRF-TOKEN"
        private const val ALL_PATHS = "/**"
        private val CORS_METHODS = listOf("GET", "POST", "PATCH", "DELETE")
        // 분석 제출(POST /v1/analyses, /v1/analyses/files)은 Idempotency-Key가 필수라 다른 출처에서도 보낼 수 있어야 한다.
        private val CORS_HEADERS = listOf("Content-Type", "X-XSRF-TOKEN", "Last-Event-ID", "Idempotency-Key")
        private val CORS_EXPOSED_HEADERS = listOf("Retry-After")
        private const val CORS_MAX_AGE_SECONDS = 3600L
        private val PUBLIC_PATHS = arrayOf(
            "/error", "/actuator/health/**", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
            "/v1/csrf", "/v1/regions", "/v1/nicknames/availability",
            "/v1/participants", "/v1/participants/**", "/v1/rankings", "/s/**",
        )
        /**
         * 모든 응답의 기본 정책. Spring Security는 응답에 이미 Content-Security-Policy가 있으면 이 값을 쓰지 않는다.
         * 그래서 공개 공유 페이지(`/s/` 아래 경로)는 컨트롤러·오류 처리기가 붙인 페이지 정책(인라인 스크립트 해시) 하나만 나간다.
         * 정책이 둘이면 브라우저가 교집합을 적용해 넘김 스크립트가 막히므로 ShareApiTest가 헤더 수를 확인한다.
         */
        private const val CONTENT_SECURITY_POLICY = "default-src 'self'; script-src 'self'; " +
            "style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'; " +
            "frame-ancestors 'none'; base-uri 'self'; object-src 'none'"
    }
}
