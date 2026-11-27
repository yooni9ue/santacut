package com.santacut.infrastructure.global.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * 분석 작업자와 원본 파일 검증 작업자의 `@Scheduled` 폴링을 켠다. Spring Boot 기본 스케줄러(스레드 1개)를 쓴다.
 * 원본 파일 검증은 이 스레드에서 하나씩 하므로 큰 원본 파일을 한 번에 하나만 메모리에 올린다.
 * 분석 작업 채점은 이 스레드가 잡아 [AnalysisWorkerConfiguration]의 채점 스레드에 넘긴다.
 * 테스트는 테스트 설정(`classpath:/config/application.yml`)의 `santacut.scheduling.enabled=false`로 모두 끈다. 켜 두면 컨텍스트가 뜰 때
 * 첫 폴링(fixedDelay는 첫 실행에 지연이 없다)이 공유 테스트 DB의 남은 작업을 잡거나 테스트 스레드의 목 스텁과 겹친다. 운영은 설정이 없어 켜진다.
 */
@Configuration
@ConditionalOnProperty(prefix = "santacut.scheduling", name = ["enabled"], havingValue = "true", matchIfMissing = true)
@EnableScheduling
class SchedulerConfiguration
