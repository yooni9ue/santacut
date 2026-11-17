package com.santacut.domain.global.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * 순위 계산과 순위표 설정. 모아서 다시 계산하는 간격(`santacut.ranking.rebuild-delay-ms`)은 예약 작업 애노테이션이 직접 읽는다.
 *
 * @property firstPageTtl 순위표 첫 페이지를 메모리에 들고 있는 시간
 * @property deliveryWindowDays 모든 참가자의 도착 예정 시각이 들어와야 하는 배송 일수(12월 25일부터, 하루 6시간)
 */
@ConfigurationProperties(prefix = "santacut.ranking")
data class RankingProperties(
    val firstPageTtl: Duration = Duration.ofSeconds(30),
    val deliveryWindowDays: Int = 7,
)
