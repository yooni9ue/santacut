package com.santacut.presentation.global.advice

/**
 * 파일 업로드가 아닌 요청 본문이 상한을 넘었다. 요청 본문 상한 필터가 던지고, 전역 예외 처리가 REQUEST_TOO_LARGE(413)로 바꾼다.
 */
class RequestBodyTooLargeException : RuntimeException()
