package com.santacut.infrastructure.global.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * 분석 작업자와 원본 파일 검증 작업자의 `@Scheduled` 폴링을 켠다. Spring Boot 기본 스케줄러(스레드 1개)를 쓴다.
 * 두 작업이 번갈아 돌므로 큰 원본 파일을 한 번에 하나만 메모리에 올린다.
 * 작업자를 직접 부르는 테스트는 `santacut.scheduling.enabled=false`로 끈다. 켜 두면 컨텍스트가 뜰 때 첫 폴링이
 * 테스트 스레드의 목 스텁과 겹쳐 엉뚱한 메서드에 답이 붙을 수 있다.
 */
@Configuration
@ConditionalOnProperty(prefix = "santacut.scheduling", name = ["enabled"], havingValue = "true", matchIfMissing = true)
@EnableScheduling
class SchedulerConfiguration
