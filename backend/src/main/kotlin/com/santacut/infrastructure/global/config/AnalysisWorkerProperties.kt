package com.santacut.infrastructure.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 분석 작업자 설정.
 *
 * @property concurrency 동시에 채점하는 작업 수. 작업 하나가 AI를 여러 번 동시에 부르므로 AI 동시 호출 수는 이 값의 몇 배다.
 */
@ConfigurationProperties(prefix = "santacut.ai.worker")
data class AnalysisWorkerProperties(
    val concurrency: Int = DEFAULT_CONCURRENCY,
) {
    companion object {
        private const val DEFAULT_CONCURRENCY = 3
    }
}
