package com.santacut.application.analysis.result

import com.santacut.domain.ranking.model.RankedResult
import java.time.Instant

/**
 * 성공한 분석 작업의 점수와 순위.
 */
data class AnalysisOutcomeResult(
    val scoreHundredths: Int,
    val verdict: String,
    val comment: String,
    val rank: Int,
    val deliverySequence: Int,
    val eta: Instant,
) {
    companion object {
        fun from(result: RankedResult): AnalysisOutcomeResult = AnalysisOutcomeResult(
            result.assessment.scoreHundredths, result.assessment.verdict, result.assessment.comment,
            result.snapshot.rank, result.snapshot.deliverySequence, result.snapshot.eta,
        )
    }
}
