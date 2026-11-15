package com.santacut.core

import jakarta.servlet.FilterChain
import jakarta.servlet.DispatcherType
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.web.filter.OncePerRequestFilter
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

fun tokenHash(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }

class SessionFilter(private val sessions: SessionRepository, private val cookieName: String) : OncePerRequestFilter() {
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val token = request.cookies?.firstOrNull { it.name == cookieName }?.value
        if (token != null && token.length in 32..128) {
            val session = sessions.findById(tokenHash(token)).orElse(null)
            if (session != null && runCatching { parseUtcDatabaseTime(session.expiresAt).isAfter(Instant.now()) }.getOrDefault(false)) {
                SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(UUID.fromString(session.userId), null, emptyList())
            }
        }
        chain.doFilter(request, response)
    }
}

@Configuration
class SecurityConfiguration(private val sessions: SessionRepository, @Value("\${app.session-secure}") private val secure: Boolean) {
    @Bean
    fun security(http: HttpSecurity): SecurityFilterChain {
        val csrf = CookieCsrfTokenRepository()
        csrf.setCookieCustomizer { it.secure(secure).sameSite("Strict").path("/") }
        http.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS).sessionAuthenticationStrategy(NullAuthenticatedSessionStrategy()) }
            .csrf { it.csrfTokenRepository(csrf) }
            .authorizeHttpRequests {
                it.dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                    .requestMatchers("/error", "/actuator/health/**", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/v1/csrf", "/v1/regions", "/v1/participants", "/v1/participants/**", "/s/**").permitAll()
                    .anyRequest().authenticated()
            }
            .addFilterBefore(SessionFilter(sessions, if (secure) "__Host-sc_session" else "sc_session"), AnonymousAuthenticationFilter::class.java)
            .exceptionHandling { it.authenticationEntryPoint { _, response, _ -> response.sendError(401) } }
            .headers { it.contentSecurityPolicy { policy -> policy.policyDirectives("default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'; object-src 'none'") } }
        return http.build()
    }
}
