package com.santacut.api.docs

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfiguration {
    @Bean
    fun santaCutOpenApi(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("SantaCut API")
                .version("v1")
                .description(
                    """SantaCut participant registration, AI assessment, ranking, delivery estimate and sharing API.

                        |Authentication flow: GET /v1/csrf, then POST /v1/participants with the returned CSRF header and cookie. The response sets an HttpOnly session cookie. Send that cookie and a fresh CSRF header for subsequent write requests. In production the session cookie is named __Host-sc_session; locally it is sc_session.
                        |
                        |Analysis accepts normalized events. Raw KakaoTalk and Instagram export files are not accepted by this API yet. Score and delivery time are entertainment estimates, not a real assessment or delivery promise.
                        |
                        |Framework error responses use JSON fields timestamp (UTC), status (HTTP status), error (reason phrase) and path (request path). Individual operations document their expected status codes.
                    """.trimMargin()
                )
                .contact(Contact().name("SantaCut"))
        )
        .components(
            Components().addSecuritySchemes(
                "sessionCookie",
                SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .`in`(SecurityScheme.In.COOKIE)
                    .name("sc_session")
                    .description("HttpOnly session cookie set by POST /v1/participants. In production its name is __Host-sc_session.")
            )
        )
}
