package com.santacut.domain.global.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * 순위 계산과 순위표 설정. 모아서 다시 계산하는 안전망 간격(`santacut.ranking.rebuild-delay-ms`)은 예약 작업 애노테이션이 직접 읽는다.
 *
 * @property firstPageTtl 순위표 첫 페이지를 메모리에 들고 있는 시간
 * @property deliveryWindowDays 모든 참가자의 도착 예정 시각이 들어와야 하는 배송 일수(12월 25일부터, 하루 6시간)
 * @property rebuildAfterChange 순위에 영향을 주는 변경이 커밋되면 예약 작업을 기다리지 않고 곧바로 다시 계산할지
 * @property rebuildDebounce 커밋 뒤 다시 계산하기 전 기다리는 시간. 이 사이에 들어온 변경은 한 번의 계산으로 모은다
 */
@ConfigurationProperties(prefix = "santacut.ranking")
data class RankingProperties(
    val firstPageTtl: Duration = Duration.ofSeconds(30),
    val deliveryWindowDays: Int = 7,
    val rebuildAfterChange: Boolean = true,
    val rebuildDebounce: Duration = Duration.ofSeconds(1),
)
