package com.santacut.presentation.global.docs

/**
 * 엔드포인트 문서 애노테이션이 함께 쓰는 값.
 */
object ApiDocs {
    const val JSON = "application/json"
    const val MULTIPART = "multipart/form-data"
    const val HTML = "text/html"
    const val SESSION_SECURITY = "sessionCookie"
    const val CSRF_HEADER = "X-XSRF-TOKEN"
    const val CSRF_HEADER_DESCRIPTION = "GET /v1/csrf가 돌려준 토큰"
    const val SHARE_TOKEN_DESCRIPTION = "43자 base64url 공유 토큰"
    const val UNAUTHORIZED_DESCRIPTION = "세션이 없거나, 잘못됐거나, 만료됨(NOT_AUTHENTICATED)"
    const val FORBIDDEN_DESCRIPTION = "CSRF 토큰이 없거나 잘못됨"
    const val SERVER_ERROR_DESCRIPTION = "예상하지 못한 서버 오류"
}
