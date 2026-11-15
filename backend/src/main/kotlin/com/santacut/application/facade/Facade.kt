package com.santacut.application.facade

import org.springframework.stereotype.Component

/**
 * 유스케이스를 조합하고 트랜잭션 경계를 정하는 application 계층 컴포넌트를 표시한다.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Component
annotation class Facade
