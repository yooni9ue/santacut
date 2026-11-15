package com.santacut.presentation.global.security

import com.santacut.application.session.facade.SessionFacade
import com.santacut.presentation.global.config.SessionCookieProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy
import org.springframework.security.web.csrf.CookieCsrfTokenRepository

/**
 * 쿠키 세션과 CSRF 쿠키로 동작하는 무상태 보안 설정.
 *
 * 세션 필터는 HTTP 진입점이라 controller처럼 [SessionFacade]로 세션을 확인한다.
 */
@Configuration
class SecurityConfiguration(
    private val sessionFacade: SessionFacade,
    private val sessionCookieProperties: SessionCookieProperties,
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
            .exceptionHandling { it.authenticationEntryPoint { _, response, _ -> response.sendError(UNAUTHORIZED) } }
            .headers { it.contentSecurityPolicy { policy -> policy.policyDirectives(CONTENT_SECURITY_POLICY) } }
        return http.build()
    }

    /** 공개 경로는 열어 두고 나머지는 로그인 세션을 요구한다. */
    private fun authorizeRequests(
        registry: AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry,
    ) {
        registry.requestMatchers(*PUBLIC_PATHS).permitAll()
            .anyRequest().authenticated()
    }

    private fun csrfTokenRepository(): CookieCsrfTokenRepository = CookieCsrfTokenRepository().apply {
        setCookieCustomizer { it.secure(sessionCookieProperties.sessionSecure).sameSite(SAME_SITE).path(ROOT_PATH) }
    }

    private fun sessionFilter(): SessionFilter =
        SessionFilter(sessionFacade, SessionCookie.name(sessionCookieProperties.sessionSecure))

    companion object {
        private val UNAUTHORIZED = HttpStatus.UNAUTHORIZED.value()
        private const val SAME_SITE = "Strict"
        private const val ROOT_PATH = "/"
        private val PUBLIC_PATHS = arrayOf(
            "/error", "/actuator/health/**", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
            "/v1/csrf", "/v1/regions", "/v1/participants", "/v1/participants/**", "/v1/rankings", "/s/**",
        )
        private const val CONTENT_SECURITY_POLICY = "default-src 'self'; script-src 'self'; " +
            "style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'; " +
            "frame-ancestors 'none'; base-uri 'self'; object-src 'none'"
    }
}
