package com.santacut.presentation.analysis.controller

import com.jayway.jsonpath.JsonPath
import com.santacut.TestParticipants.REGION_CODE
import com.santacut.TestParticipants.uniqueNickname
import com.santacut.application.participant.command.RegisterParticipantCommand
import com.santacut.application.participant.facade.ParticipantFacade
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.UUID
import kotlin.test.assertEquals

/**
 * 업로드 크기 제한은 MockMvc가 아니라 실제 서블릿 컨테이너가 검사하므로 내장 서버를 띄워 확인한다.
 * 제한을 1KB로 줄이고 2KB 파일을 올린다.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.datasource.url=jdbc:sqlite:./build/upload-limit-test.db",
        "app.season-year=2099",
        "santacut.ai.poll-delay-ms=3600000",
        "santacut.upload.poll-delay-ms=3600000",
        "spring.servlet.multipart.max-file-size=1KB",
        "spring.servlet.multipart.max-request-size=10KB",
    ],
)
class FileTooLargeApiTest @Autowired constructor(
    private val participantFacade: ParticipantFacade,
    @LocalServerPort private val port: Int,
) {
    private val client: HttpClient = HttpClient.newHttpClient()

    @Test
    fun `파일이 제한보다 크면 413과 FILE_TOO_LARGE다`() {
        val sessionToken = participantFacade.register(RegisterParticipantCommand(uniqueNickname(), REGION_CODE)).sessionToken
        val csrf = client.send(HttpRequest.newBuilder(uri("/v1/csrf")).build(), HttpResponse.BodyHandlers.ofString())
        val csrfCookie = csrf.headers().firstValue(SET_COOKIE).orElseThrow().substringBefore(";")

        val response = client.send(uploadRequest(sessionToken, csrfCookie, JsonPath.read(csrf.body(), "$.token")), HttpResponse.BodyHandlers.ofString())

        assertEquals(PAYLOAD_TOO_LARGE, response.statusCode())
        assertEquals("FILE_TOO_LARGE", JsonPath.read(response.body(), "$.code"))
    }

    private fun uploadRequest(sessionToken: String, csrfCookie: String, csrfToken: String): HttpRequest =
        HttpRequest.newBuilder(uri("/v1/analyses/files"))
            .header("Content-Type", "multipart/form-data; boundary=$BOUNDARY")
            .header("Cookie", "$SESSION_COOKIE=$sessionToken; $csrfCookie")
            .header("X-XSRF-TOKEN", csrfToken)
            .header("Idempotency-Key", "key-" + UUID.randomUUID())
            .POST(HttpRequest.BodyPublishers.ofString(multipartBody()))
            .build()

    private fun multipartBody(): String =
        part("source", "", "KAKAO") + part("self_speaker", "", "santa") +
            part("file", "; filename=\"chat.txt\"", "a".repeat(OVERSIZED_FILE_BYTES)) + "--$BOUNDARY--\r\n"

    private fun part(name: String, fileName: String, value: String): String =
        "--$BOUNDARY\r\nContent-Disposition: form-data; name=\"$name\"$fileName\r\n\r\n$value\r\n"

    private fun uri(path: String): URI = URI.create("http://localhost:$port$path")

    companion object {
        private const val BOUNDARY = "santacut-boundary"
        private const val SESSION_COOKIE = "sc_session"
        private const val SET_COOKIE = "Set-Cookie"
        private const val PAYLOAD_TOO_LARGE = 413
        private const val OVERSIZED_FILE_BYTES = 2048
    }
}
