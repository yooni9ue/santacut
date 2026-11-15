package com.santacut.domain.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * AI 호출 예산 설정. 시즌 연도는 [AppProperties.seasonYear] 한 곳에서만 정한다.
 * 분석 작업자 대기 시간(`santacut.ai.poll-delay-ms`)은 `AnalysisWorkerFacade`의 `@Scheduled`가 직접 읽는다.
 */
@ConfigurationProperties(prefix = "santacut")
data class SantaCutProperties(
    val ai: Ai = Ai(),
) {
    /**
     * @property dailyCallLimit 하루(서울 기준) AI 호출 상한
     */
    data class Ai(
        val dailyCallLimit: Int = DEFAULT_DAILY_CALL_LIMIT,
    )

    companion object {
        private const val DEFAULT_DAILY_CALL_LIMIT = 100
    }
}
