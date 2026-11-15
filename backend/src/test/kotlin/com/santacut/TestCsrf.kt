package com.santacut

import com.jayway.jsonpath.JsonPath
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

/**
 * 쓰기 요청에 실제 CSRF 쿠키와 헤더를 붙인다. GET /v1/csrf가 준 값을 그대로 쓴다.
 * 일반 요청과 multipart 요청 빌더 모두에 쓸 수 있다.
 *
 * spring-security-test의 csrf() 후처리기는 공유 보안 필터의 토큰 저장소를 세션 저장소로 바꿔 놓는다.
 * 같은 컨텍스트의 뒤 테스트가 쿠키 저장소 계약(X-XSRF-TOKEN)을 볼 수 없게 되므로 통합 테스트에서는 이 함수를 쓴다.
 */
object TestCsrf {
    private const val CSRF_PATH = "/v1/csrf"
    private const val CSRF_COOKIE = "XSRF-TOKEN"

    fun <B : AbstractMockHttpServletRequestBuilder<B>> withCsrf(mockMvc: MockMvc, request: B): B {
        val response = mockMvc.perform(get(CSRF_PATH)).andReturn().response
        val body = response.contentAsString
        return request
            .cookie(checkNotNull(response.getCookie(CSRF_COOKIE)))
            .header(JsonPath.read(body, "$.headerName"), JsonPath.read<String>(body, "$.token"))
    }
}
