package com.santacut.infrastructure.global.config

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * 분석 작업자의 `@Scheduled` 폴링을 켠다. 예약 작업은 그 하나뿐이라 Spring Boot 기본 스케줄러(스레드 1개)로 충분하다.
 */
@Configuration
@EnableScheduling
class SchedulerConfiguration
