package com.santacut.infrastructure.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

/**
 * 분석 작업을 채점하는 스레드 풀. 스레드 수가 동시에 채점하는 작업 수([AnalysisWorkerProperties.concurrency])다.
 * 예약 폴링 스레드와 따로 두어, 채점이 오래 걸려도 원본 파일 검증과 스트림 heartbeat가 밀리지 않게 한다.
 * 앱이 꺼질 때 채점 중인 작업은 리스가 끝난 뒤 다른 시도로 다시 잡힌다.
 */
@Configuration
class AnalysisWorkerConfiguration {
    @Bean
    fun analysisWorkerExecutor(properties: AnalysisWorkerProperties): ThreadPoolTaskExecutor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = properties.concurrency
            maxPoolSize = properties.concurrency
            setThreadNamePrefix(THREAD_NAME_PREFIX)
        }

    companion object {
        private const val THREAD_NAME_PREFIX = "analysis-worker-"
    }
}
