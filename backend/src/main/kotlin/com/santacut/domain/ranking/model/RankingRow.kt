package com.santacut.domain.ranking.model

import java.time.Instant

/**
 * 순위표 한 줄. 리포지토리 조회 결과를 바로 담는다.
 *
 * @property deliverySequence 다음 페이지 cursor를 만드는 배송 순서
 */
data class RankingRow(
    val nickname: String,
    val rank: Int,
    val tied: Boolean,
    val scoreHundredths: Int,
    val eta: Instant,
    val deliverySequence: Int,
)
