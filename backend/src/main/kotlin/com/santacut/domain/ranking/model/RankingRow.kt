package com.santacut.domain.ranking.model

import java.time.Instant

/**
 * 순위표 한 줄. 리포지토리 조회 결과를 바로 담는다.
 */
data class RankingRow(
    val nickname: String,
    val rank: Int,
    val scoreHundredths: Int,
    val eta: Instant,
    val computedAt: Instant,
)
