package com.santacut.presentation.global.docs

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
        .info(Info().title(TITLE).version(VERSION).description(DESCRIPTION).contact(Contact().name(CONTACT_NAME)))
        .components(Components().addSecuritySchemes(ApiDocs.SESSION_SECURITY, sessionCookieScheme()))

    private fun sessionCookieScheme(): SecurityScheme = SecurityScheme()
        .type(SecurityScheme.Type.APIKEY)
        .`in`(SecurityScheme.In.COOKIE)
        .name(LOCAL_SESSION_COOKIE)
        .description("POST /v1/participants가 설정하는 HttpOnly 세션 쿠키입니다. 운영 환경에서 이름은 __Host-sc_session입니다.")

    companion object {
        private const val TITLE = "SantaCut API"
        private const val VERSION = "v1"
        private const val CONTACT_NAME = "SantaCut"
        private const val LOCAL_SESSION_COOKIE = "sc_session"
        private val DESCRIPTION = """
            산타컷 참가자 등록, AI 평가, 순위, 배송 예정 시각, 공유 API입니다.

            인증 흐름: GET /v1/csrf로 토큰을 받은 뒤, 받은 CSRF 헤더와 쿠키를 붙여 POST /v1/participants를 호출합니다. 응답은 HttpOnly 세션 쿠키를 설정합니다. 이후 쓰기 요청에는 그 쿠키와 새 CSRF 헤더를 함께 보냅니다. 운영 환경의 세션 쿠키 이름은 __Host-sc_session이고 로컬에서는 sc_session입니다.

            분석은 두 방법으로 제출합니다. POST /v1/analyses는 브라우저가 정규화한 이벤트를 JSON으로 받고, POST /v1/analyses/files는 카카오톡 TXT나 인스타그램 message_1.json 원본 파일을 multipart로 받아 서버가 검증·정규화합니다. 두 방법 모두 202와 작업 ID를 돌려주고, 하루 제출 한도와 Idempotency-Key를 함께 씁니다. 점수와 배송 시각은 재미를 위한 추정이며 실제 평가나 배송 약속이 아닙니다.

            오류 응답은 JSON 필드 timestamp(UTC), status(HTTP 상태), error(상태 문구), path(요청 경로)를 씁니다. CSRF 오류(403)나 없는 경로 같은 프레임워크 오류를 뺀 모든 오류에 code(오류 코드)와 message(한국어 메시지)가 더 붙고, 요청 한도 초과에는 retry_at(다시 시도할 수 있는 시각)도 붙습니다. 각 엔드포인트에 발생 가능한 상태 코드를 적어 두었습니다.
        """.trimIndent()
    }
}
