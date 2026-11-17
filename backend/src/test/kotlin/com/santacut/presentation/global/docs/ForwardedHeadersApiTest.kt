package com.santacut.presentation.global.docs

import com.jayway.jsonpath.JsonPath
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.assertEquals

/**
 * 운영 프로필은 TLS를 끝낸 Caddy가 붙인 X-Forwarded-Proto를 믿는다. 헤더는 실제 Tomcat(RemoteIpValve)이 읽으므로 내장 서버를 띄워 확인한다.
 * 테스트 요청은 루프백에서 오므로 믿는 프록시 대역에 든다.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.datasource.url=jdbc:sqlite:./build/forwarded-headers-test.db",
        "app.season-year=2099",
        "santacut.ai.poll-delay-ms=3600000",
        "santacut.upload.poll-delay-ms=3600000",
        "SWAGGER_ENABLED=true",
    ],
)
@ActiveProfiles("production")
class ForwardedHeadersApiTest @Autowired constructor(
    @LocalServerPort private val port: Int,
) {
    private val client: HttpClient = HttpClient.newHttpClient()

    @Test
    fun `프록시가 X-Forwarded-Proto https를 붙이면 OpenAPI 서버 주소가 https다`() {
        val request = HttpRequest.newBuilder(URI.create("http://localhost:$port/v3/api-docs"))
            .header(FORWARDED_PROTO, HTTPS)
            .header(FORWARDED_HOST, PUBLIC_HOST)
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        assertEquals(OK, response.statusCode())
        assertEquals("$HTTPS://$PUBLIC_HOST", JsonPath.read(response.body(), "$.servers[0].url"))
    }

    companion object {
        private const val FORWARDED_PROTO = "X-Forwarded-Proto"
        private const val FORWARDED_HOST = "X-Forwarded-Host"
        private const val HTTPS = "https"
        private const val PUBLIC_HOST = "api-dev.santacut.me"
        private const val OK = 200
    }
}
