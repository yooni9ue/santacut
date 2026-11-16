package com.santacut.presentation.global.docs

/**
 * 엔드포인트 문서 애노테이션이 함께 쓰는 값.
 */
object ApiDocs {
    const val JSON = "application/json"
    const val MULTIPART = "multipart/form-data"
    const val HTML = "text/html"
    const val EVENT_STREAM = "text/event-stream"
    const val SESSION_SECURITY = "sessionCookie"
    const val CSRF_HEADER = "X-XSRF-TOKEN"
    const val CSRF_HEADER_DESCRIPTION = "GET /v1/csrf가 돌려준 토큰"
    const val SHARE_TOKEN_DESCRIPTION = "43자 base64url 공유 토큰"
    const val UNAUTHORIZED_DESCRIPTION = "세션이 없거나, 잘못됐거나, 만료됨(NOT_AUTHENTICATED)"
    const val FORBIDDEN_DESCRIPTION = "CSRF 토큰이 없거나 잘못됨"
    const val SERVER_ERROR_DESCRIPTION = "예상하지 못한 서버 오류"
    const val PROMPT_ADMIN_NOTE = "dev 전용 관리자 API입니다. santacut.prompt-admin.enabled=true(PROMPT_ADMIN_ENABLED)일 때만 있고, " +
        "운영에서는 꺼져 있어 404입니다. 인증은 없고 쓰기 요청에는 CSRF 토큰이 필요합니다. 프론트엔드용이 아닙니다."
    const val PROMPT_ADMIN_DISABLED_DESCRIPTION = "관리자 API가 꺼져 있음(santacut.prompt-admin.enabled가 true가 아님). code 없는 프레임워크 404"
    const val PROMPT_NAME_DESCRIPTION = "프롬프트 이름. 영문 소문자, 숫자, 하이픈으로 된 1~40자(예: score)"
}
