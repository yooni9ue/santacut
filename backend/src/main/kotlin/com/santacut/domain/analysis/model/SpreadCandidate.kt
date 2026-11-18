package com.santacut.domain.analysis.model

import java.time.YearMonth

/**
 * 달마다 고르게 고를 후보 하나([SpreadCandidates]의 원소).
 *
 * @property month 후보가 속한 달(한국 시각)
 * @property cost 예산에서 쓰는 몫. 0이면 예산과 상관없이 늘 남긴다.
 */
data class SpreadCandidate<T>(
    val value: T,
    val month: YearMonth,
    val cost: Int,
)
