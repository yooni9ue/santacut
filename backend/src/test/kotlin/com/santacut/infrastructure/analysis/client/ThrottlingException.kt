package com.santacut.infrastructure.analysis.client

/**
 * AWS SDK의 요청 한도 예외와 이름만 같은 테스트용 예외. [ExceptionCauses]는 예외 클래스 이름으로 제공자 오류를 알아본다.
 */
class ThrottlingException : RuntimeException("요청 한도 초과")
