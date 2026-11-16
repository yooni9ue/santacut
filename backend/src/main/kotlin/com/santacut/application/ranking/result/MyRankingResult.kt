package com.santacut.application.ranking.result

import com.santacut.domain.ranking.model.RankedResult
import com.santacut.domain.ranking.model.RankingStage
import java.time.Instant

/**
 * 로그인한 참가자의 최신 점수, 순위, 배송 예정 시각.
 * 첫 순위 계산 전이면 순위 관련 값(rank, deliverySequence, eta, computedAt)이 null이다.
 *
 * @property stage 순위 확정 상태 이름(PREVIEW, FINAL)
 * @property rankingPending 최신 평가가 아직 순위에 반영되지 않았는지
 * @property lowConfidenceDimensions 최신 평가에서 근거가 부족해 중립 점수를 준 항목 키
 */
data class MyRankingResult(
    val rank: Int?,
    val tied: Boolean,
    val deliverySequence: Int?,
    val eta: Instant?,
    val computedAt: Instant?,
    val scoreHundredths: Int,
    val verdict: String,
    val comment: String,
    val stage: String,
    val rankingPending: Boolean,
    val lowConfidenceDimensions: List<String>,
) {
    companion object {
        fun from(result: RankedResult, stage: RankingStage): MyRankingResult = MyRankingResult(
            result.snapshot?.rank, result.snapshot?.tied ?: false, result.snapshot?.deliverySequence,
            result.snapshot?.eta, result.snapshot?.computedAt,
            result.assessment.scoreHundredths, result.assessment.verdict, result.assessment.comment,
            stage.name, result.rankingPending, result.assessment.lowConfidenceKeys(),
        )
    }
}
