package com.santacut

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc

/**
 * 통합 테스트 공통 설정. 모든 통합 테스트가 같은 애플리케이션 컨텍스트와 `build/test.db` 파일을 함께 쓴다.
 *
 * 시즌을 먼 미래로 미뤄 실행 날짜와 상관없이 분석을 받을 수 있게 하고,
 * 분석 작업자가 테스트 도중 AI를 부르지 않도록 대기 시간을 길게 잡는다. dev처럼 프롬프트 관리 API를 켠다.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:sqlite:./build/test.db",
        "app.season-year=2099",
        "santacut.ai.poll-delay-ms=3600000",
        "santacut.upload.poll-delay-ms=3600000",
        "santacut.prompt-admin.enabled=true",
        "santacut.prompt-admin.token=" + IntegrationTest.ADMIN_TOKEN,
    ],
)
@AutoConfigureMockMvc
annotation class IntegrationTest {
    companion object {
        /** 통합 테스트가 켜는 프롬프트 관리 API의 관리 토큰. */
        const val ADMIN_TOKEN = "test-admin-token"
    }
}
