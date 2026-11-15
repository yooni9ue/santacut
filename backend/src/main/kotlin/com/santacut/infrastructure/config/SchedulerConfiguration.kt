package com.santacut.infrastructure.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler

@Configuration
@EnableScheduling
class SchedulerConfiguration {
    @Bean
    fun taskScheduler(): ThreadPoolTaskScheduler = ThreadPoolTaskScheduler().apply {
        poolSize = POOL_SIZE
        setThreadNamePrefix(THREAD_NAME_PREFIX)
        setRemoveOnCancelPolicy(true)
    }

    companion object {
        private const val POOL_SIZE = 4
        private const val THREAD_NAME_PREFIX = "santacut-scheduler-"
    }
}
