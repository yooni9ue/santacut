package com.santacut.domain.global.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * 참가자별 지역 변경 한도.
 *
 * @property maxPerWindow [window] 동안 바꿀 수 있는 횟수
 * @property window 짧은 한도를 세는 창 길이
 * @property maxPerSeason 한 시즌 동안 바꿀 수 있는 횟수
 */
@ConfigurationProperties(prefix = "santacut.region-change")
data class RegionChangeProperties(
    val maxPerWindow: Int = 2,
    val window: Duration = Duration.ofHours(24),
    val maxPerSeason: Int = 5,
)
