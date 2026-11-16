package com.santacut.domain.global.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * 진행 상황 스트림(SSE) 상한 설정.
 *
 * @property maxOpenStreams 앱 전체에서 동시에 열 수 있는 스트림 수
 * @property retryAfter 상한에 걸린 요청에 Retry-After로 알릴 대기 시간
 */
@ConfigurationProperties(prefix = "santacut.stream")
data class AnalysisStreamProperties(
    val maxOpenStreams: Int = 200,
    val retryAfter: Duration = Duration.ofSeconds(30),
)
