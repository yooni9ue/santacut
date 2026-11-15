package com.santacut.domain.ranking.model

import com.santacut.domain.analysis.model.Assessment

/**
 * 참가자 한 명의 평가와 그 평가로 정해진 순위 스냅숏.
 * 점수와 순위는 늘 같은 평가 것을 묶는다. 서로 다른 평가의 점수와 순위를 섞지 않는다.
 *
 * @property snapshot 최신 순위 스냅숏. 첫 순위 계산 전이면 null이고 [assessment]는 최신 평가다.
 * @property rankingPending 최신 평가가 아직 순위에 반영되지 않았는지
 */
class RankedResult(
    val snapshot: ResultSnapshot?,
    val assessment: Assessment,
    val rankingPending: Boolean,
)
