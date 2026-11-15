package com.santacut.infrastructure.global.config

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * 분석 작업자와 원본 파일 검증 작업자의 `@Scheduled` 폴링을 켠다. Spring Boot 기본 스케줄러(스레드 1개)를 쓴다.
 * 두 작업이 번갈아 돌므로 큰 원본 파일을 한 번에 하나만 메모리에 올린다.
 */
@Configuration
@EnableScheduling
class SchedulerConfiguration
