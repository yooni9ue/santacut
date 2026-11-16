package com.santacut.presentation.global.security

import com.santacut.application.session.facade.SessionFacade
import com.santacut.presentation.global.config.SessionCookieProperties
import jakarta.servlet.DispatcherType
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.AuthenticationException
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.web.servlet.HandlerExceptionResolver

/**
 * 쿠키 세션과 CSRF 쿠키로 동작하는 무상태 보안 설정.
 *
 * 세션 필터는 HTTP 진입점이라 controller처럼 [SessionFacade]로 세션을 확인한다.
 * 인증 실패는 [HandlerExceptionResolver]로 넘겨 다른 오류와 같은 JSON 본문(code NOT_AUTHENTICATED)으로 응답한다.
 */
@Configuration
class SecurityConfiguration(
    private val sessionFacade: SessionFacade,
    private val sessionCookieProperties: SessionCookieProperties,
    @param:Qualifier(HANDLER_EXCEPTION_RESOLVER) private val handlerExceptionResolver: HandlerExceptionResolver,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.sessionManagement {
            it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .sessionAuthenticationStrategy(NullAuthenticatedSessionStrategy())
        }
            .csrf { it.csrfTokenRepository(csrfTokenRepository()) }
            .authorizeHttpRequests(::authorizeRequests)
            .addFilterBefore(sessionFilter(), AnonymousAuthenticationFilter::class.java)
            .exceptionHandling { it.authenticationEntryPoint(::rejectUnauthenticated) }
            .headers { it.contentSecurityPolicy { policy -> policy.policyDirectives(CONTENT_SECURITY_POLICY) } }
        return http.build()
    }

    /**
     * 공개 경로는 열어 두고 나머지는 로그인 세션을 요구한다. 공개 경로도 쓰기 요청은 CSRF 토큰이 있어야 한다.
     * 비동기 디스패치(SSE 스트림이 끝날 때 다시 들어오는 요청)는 처음 요청에서 이미 인증했으므로 다시 검사하지 않는다.
     */
    private fun authorizeRequests(
        registry: AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry,
    ) {
        registry.dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
            .requestMatchers(*PUBLIC_PATHS).permitAll()
            .anyRequest().authenticated()
    }

    private fun csrfTokenRepository(): CookieCsrfTokenRepository = CookieCsrfTokenRepository().apply {
        setCookieCustomizer { it.secure(sessionCookieProperties.sessionSecure).sameSite(SAME_SITE).path(ROOT_PATH) }
    }

    private fun rejectUnauthenticated(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException,
    ) {
        handlerExceptionResolver.resolveException(request, response, null, exception)
    }

    private fun sessionFilter(): SessionFilter =
        SessionFilter(sessionFacade, SessionCookie.name(sessionCookieProperties.sessionSecure))

    companion object {
        private const val HANDLER_EXCEPTION_RESOLVER = "handlerExceptionResolver"
        private const val SAME_SITE = "Strict"
        private const val ROOT_PATH = "/"
        private val PUBLIC_PATHS = arrayOf(
            "/error", "/actuator/health/**", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
            "/v1/csrf", "/v1/regions", "/v1/nicknames/availability",
            "/v1/participants", "/v1/participants/**", "/v1/rankings", "/s/**",
            // dev 전용 프롬프트 관리 API. 꺼져 있으면 컨트롤러가 없어 404다. 쓰기 요청은 다른 경로처럼 CSRF를 검사한다.
            "/v1/admin/prompts/**",
        )
        private const val CONTENT_SECURITY_POLICY = "default-src 'self'; script-src 'self'; " +
            "style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'; " +
            "frame-ancestors 'none'; base-uri 'self'; object-src 'none'"
    }
}
