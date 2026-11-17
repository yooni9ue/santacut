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
    const val PROMPT_ADMIN_NOTE = "dev 전용 관리자 API입니다. santacut.prompt-admin.enabled=true(PROMPT_ADMIN_ENABLED)이고 " +
        "관리 토큰(PROMPT_ADMIN_TOKEN)을 설정했을 때만 있고, 운영에서는 꺼져 있어 404입니다. " +
        "모든 요청에 X-Admin-Token 헤더가 필요하고, 쓰기 요청에는 CSRF 토큰도 필요합니다. 프론트엔드용이 아닙니다."
    const val PROMPT_ADMIN_DISABLED_DESCRIPTION =
        "관리자 API가 꺼져 있음(santacut.prompt-admin.enabled가 true가 아니거나 관리 토큰이 없음). code 없는 프레임워크 404"
    const val ADMIN_TOKEN_HEADER = "X-Admin-Token"
    const val ADMIN_TOKEN_HEADER_DESCRIPTION = "서버 설정 PROMPT_ADMIN_TOKEN과 같은 관리 토큰"
    const val ADMIN_TOKEN_REJECTED_DESCRIPTION = "X-Admin-Token이 없거나 틀림(NOT_AUTHENTICATED)"
    const val ADMIN_FORBIDDEN_DESCRIPTION = "참가자 세션으로 X-Admin-Token 없이 부름, 또는 쓰기 요청의 CSRF 토큰이 없거나 잘못됨"
    const val PROMPT_NAME_DESCRIPTION = "프롬프트 이름. 영문 소문자, 숫자, 하이픈으로 된 1~40자(예: score)"
}
