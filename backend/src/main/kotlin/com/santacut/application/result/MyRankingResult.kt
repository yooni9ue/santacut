package com.santacut.application.result

import com.santacut.domain.model.RankedResult
import java.time.Instant

/**
 * 로그인한 참가자의 최신 점수, 순위, 배송 예정 시각.
 */
data class MyRankingResult(
    val rank: Int,
    val deliverySequence: Int,
    val eta: Instant,
    val computedAt: Instant,
    val scoreHundredths: Int,
    val verdict: String,
    val comment: String,
) {
    companion object {
        fun from(result: RankedResult): MyRankingResult = MyRankingResult(
            result.snapshot.rank, result.snapshot.deliverySequence, result.snapshot.eta, result.snapshot.computedAt,
            result.assessment.scoreHundredths, result.assessment.verdict, result.assessment.comment,
        )
    }
}
