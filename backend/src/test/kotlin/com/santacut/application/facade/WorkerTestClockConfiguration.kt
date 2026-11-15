package com.santacut.application.facade

import com.santacut.MutableClock
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.time.Instant

/**
 * 분석 작업자 통합 테스트에서 시즌 마감 직전·직후로 시각을 옮기기 위해 기본 시계 대신 쓰는 시계.
 */
@TestConfiguration
class WorkerTestClockConfiguration {
    @Bean
    @Primary
    fun mutableClock(): MutableClock = MutableClock(Instant.EPOCH)
}
