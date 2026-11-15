package com.santacut.infrastructure.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.time.ZoneOffset

@Configuration
class ClockConfiguration {
    /**
     * 데이터베이스 시각 문자열이 밀리초까지만 저장되므로 시계도 밀리초 단위로 맞춘다.
     */
    @Bean
    fun clock(): Clock = Clock.tickMillis(ZoneOffset.UTC)
}
