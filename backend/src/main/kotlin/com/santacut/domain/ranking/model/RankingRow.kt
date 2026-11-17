package com.santacut.domain.ranking.model

import com.santacut.domain.global.model.SeoulTime
import java.time.Instant
import java.time.LocalDate

/**
 * 순위표 한 줄. 리포지토리 조회 결과를 바로 담는다.
 * 다른 참가자에게 보이는 줄이므로 도착 예정은 날짜([etaDate])로만 내보낸다. 정확한 시각의 간격으로 지역 사이 거리를 거꾸로 셈하지 못하게 하기 위해서다.
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
) {
    /** 서울 날짜로 줄인 도착 예정일. */
    fun etaDate(): LocalDate = eta.atZone(SeoulTime.ZONE).toLocalDate()
}
