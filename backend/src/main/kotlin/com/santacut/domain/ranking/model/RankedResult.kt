package com.santacut.domain.ranking.model

import com.santacut.domain.analysis.model.Assessment

/**
 * 참가자 한 명의 평가와 그 평가로 정해진 순위 스냅숏.
 * 점수와 순위는 늘 같은 평가 것을 묶는다. 서로 다른 평가의 점수와 순위를 섞지 않는다.
 *
 * @property snapshot 최신 순위 스냅숏. 첫 순위 계산 전이면 null이고 [assessment]는 최신 평가다.
 * @property rankingPending 최신 평가가 아직 순위에 반영되지 않았는지
 */
class RankedResult private constructor(
    val snapshot: ResultSnapshot?,
    val assessment: Assessment,
    val rankingPending: Boolean,
) {
    companion object {
        /**
         * 최신 평가 [latest]와 최신 순위 스냅숏 [snapshot]으로 보여 줄 점수와 순위를 정한다.
         * 스냅숏이 최신 평가 것이면 둘을 묶고 반영 완료다.
         * 스냅숏이 이전 평가 것이면 [findAssessment]로 찾은 그 평가와 묶고 반영 대기다.
         * 스냅숏이 없거나 그 평가를 찾을 수 없으면 순위 없이 최신 평가만 보여 주고 반영 대기다.
         */
        fun of(
            latest: Assessment,
            snapshot: ResultSnapshot?,
            findAssessment: (String) -> Assessment?,
        ): RankedResult {
            if (snapshot == null) return unranked(latest)
            if (snapshot.assessmentId == latest.id) return RankedResult(snapshot, latest, false)
            val ranked = findAssessment(snapshot.assessmentId) ?: return unranked(latest)
            return RankedResult(snapshot, ranked, true)
        }

        private fun unranked(latest: Assessment): RankedResult = RankedResult(null, latest, true)
    }
}
