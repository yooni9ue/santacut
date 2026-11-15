package com.santacut

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler

@Configuration
class SchedulerConfiguration {
    @Bean
    fun taskScheduler(): ThreadPoolTaskScheduler = ThreadPoolTaskScheduler().apply {
        poolSize = 4
        setThreadNamePrefix("santacut-scheduler-")
        setRemoveOnCancelPolicy(true)
    }
}
