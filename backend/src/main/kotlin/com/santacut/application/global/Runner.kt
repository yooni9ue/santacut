package com.santacut.application.global

import org.springframework.stereotype.Component

/**
 * facade가 쓰는 실행 기법(트랜잭션 나누기, 스레드·슬롯, 잠금, 리스, 재시도)을 이름 있는 단계로 감싸는 application 계층 컴포넌트를 표시한다.
 * runner는 facade나 다른 runner를 주입받지 않는다.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Component
annotation class Runner
