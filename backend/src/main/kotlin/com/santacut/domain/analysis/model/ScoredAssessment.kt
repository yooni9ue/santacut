package com.santacut.domain.analysis.model

/**
 * 평가로 저장할 채점 결과 묶음.
 *
 * @property promptVersion 점수를 매길 때 쓴 채점 프롬프트 판 번호
 * @property quote 본인 근거 줄에서 고른 20자 이하 한마디. 없으면 null이다.
 * @property badges 본인 말에서 받은 배지
 */
data class ScoredAssessment(
    val score: Score,
    val promptVersion: Int,
    val quote: String?,
    val badges: EarnedBadges,
)
