package com.santacut.domain.ranking.model

import com.santacut.domain.analysis.model.Assessment

/**
 * 참가자 한 명의 평가와 그 평가로 정해진 순위 스냅숏.
 */
class RankedResult(
    val snapshot: ResultSnapshot,
    val assessment: Assessment,
)
