package com.santacut.application.ranking.result

import com.santacut.domain.ranking.model.RankedResult
import com.santacut.domain.ranking.model.RankingStage
import com.santacut.domain.ranking.model.ResultProfile
import java.time.Instant

/**
 * 로그인한 참가자의 최신 점수, 순위, 배송 예정 시각.
 * 첫 순위 계산 전이면 순위 관련 값(rank, deliverySequence, eta, computedAt)이 null이다.
 *
 * @property stage 순위 확정 상태 이름(PREVIEW, FINAL)
 * @property rankingPending 최신 평가가 아직 순위에 반영되지 않았는지
 * @property lowConfidenceDimensions 최신 평가에서 근거가 부족해 사전값(중립 점수 55와 근거 있는 다른 항목 평균들의 평균)을 준 항목 키
 * @property archetypeCode 재미용 산타 유형 코드([com.santacut.domain.analysis.model.Archetype] 이름)
 * @property archetypeName 산타 유형 한국어 이름
 * @property dimensionBars 항목 막대 4개
 * @property weakestDimension 점수가 가장 낮은 항목
 * @property topPercent 상위 몇 %. 참가자 100명 미만이거나 순위가 없으면 null이다.
 * @property archetypeSharePercent 같은 유형 참가자 비율(%). 참가자가 30명보다 적으면 null이다.
 * @property badges 본인 말에서 받은 배지 라벨
 * @property quote 본인 근거 줄의 20자 이하 한마디. 없으면 null이다.
 * @property dossierAvailable 이 결과(평가)에 산타 조서가 있는지. 이 기능 전 평가면 거짓이다.
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
    val archetypeCode: String,
    val archetypeName: String,
    val dimensionBars: DimensionBarResults,
    val weakestDimension: DimensionBarResult,
    val topPercent: Int?,
    val archetypeSharePercent: Int?,
    val badges: List<String>,
    val quote: String?,
    val dossierAvailable: Boolean,
) {
    companion object {
        fun from(result: RankedResult, profile: ResultProfile, stage: RankingStage, dossierAvailable: Boolean): MyRankingResult = MyRankingResult(
            result.snapshot?.rank, result.snapshot?.tied ?: false, result.snapshot?.deliverySequence,
            result.snapshot?.eta, result.snapshot?.computedAt,
            result.assessment.scoreHundredths, result.assessment.verdict, result.assessment.comment,
            stage.name, result.rankingPending, result.assessment.lowConfidenceKeys(),
            profile.archetype.name, profile.archetype.koreanName, DimensionBarResults.from(profile.dimensionScores),
            DimensionBarResult.of(profile.weakestDimension(), profile.dimensionScores), profile.topPercent,
            profile.archetypeSharePercent, profile.badges, profile.quote, dossierAvailable,
        )
    }
}
