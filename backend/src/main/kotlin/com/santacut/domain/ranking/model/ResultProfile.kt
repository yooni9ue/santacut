package com.santacut.domain.ranking.model

import com.santacut.domain.analysis.model.Archetype
import com.santacut.domain.analysis.model.ArchetypeCounts
import com.santacut.domain.analysis.model.Assessment
import com.santacut.domain.analysis.model.ScoreComponents
import com.santacut.domain.analysis.model.ScoreDimension

/**
 * 결과 화면과 공유 카드에 보여 줄 재미용 꾸밈 값. 순위와 점수에는 영향을 주지 않는다.
 * 장난기 기준값과 유형 비율은 가장 최근 순위 계산 때 저장한 [ResultPopulation], 상위 비율은 그 순위의 참가자 수로 낸다.
 *
 * @property topPercent 상위 몇 %인지. 참가자가 [MIN_PARTICIPANTS_FOR_TOP_PERCENT]명보다 적거나 순위가 없으면 null이다.
 * @property rankedTopPercent 리캡 카드 상단 바에 싣는 상위 몇 %. 참가자 수 기준 없이 순위와 그 순위 계산의 참가자 수로 내고, 순위가 없을 때만 null이다.
 * @property participantCount 그 순위 계산에 든 참가자 수. 순위가 없으면 0이다.
 * @property archetypeSharePercent 같은 유형인 참가자 비율(%). 참가자가 [ArchetypeCounts.MIN_PARTICIPANTS_FOR_SHARE]명보다 적으면 null이다.
 * @property badges 본인 말에서 받은 배지 라벨
 * @property quote 본인 근거 줄의 20자 이하 한마디. 상대 말은 들어가지 않는다.
 */
class ResultProfile(
    val archetype: Archetype,
    val dimensionScores: ScoreComponents,
    val topPercent: Int?,
    val rankedTopPercent: Int?,
    val participantCount: Long,
    val archetypeSharePercent: Int?,
    val badges: List<String>,
    val quote: String?,
) {
    /** 점수가 가장 낮은 항목. */
    fun weakestDimension(): ScoreDimension = dimensionScores.rankedDimensions().last()

    /** 공유 카드에 실을 첫 배지. 없으면 null이다. */
    fun firstBadge(): String? = badges.getOrNull(0)

    companion object {
        const val MIN_PARTICIPANTS_FOR_TOP_PERCENT = 100
        private const val PERCENT = 100

        /**
         * [assessment]와 그 순위 [snapshot]으로 꾸밈 값을 만든다. [population]은 저장한 장난기 기준값과 유형 분포,
         * [participantCount]는 그 순위 계산에 든 참가자 수다.
         */
        fun of(assessment: Assessment, snapshot: ResultSnapshot?, population: ResultPopulation, participantCount: Long): ResultProfile {
            val archetype = population.archetypeOf(assessment)
            val rankedTopPercent = topPercentOf(snapshot?.rank, participantCount)
            return ResultProfile(
                archetype, assessment.dimensionScores(), rankedTopPercent.takeIf { participantCount >= MIN_PARTICIPANTS_FOR_TOP_PERCENT },
                rankedTopPercent, participantCount,
                population.sharePercentOf(archetype), assessment.badgeLabels(), assessment.quote,
            )
        }

        /** 상위 비율(올림, 1~100). 공유 링크가 붙잡아 남은 옛 스냅숏은 그 계산의 행이 일부만 남아 순위가 참가자 수보다 클 수 있다. */
        private fun topPercentOf(rank: Int?, participantCount: Long): Int? {
            if (rank == null || participantCount <= 0) return null
            return ((rank * PERCENT + participantCount - 1) / participantCount).toInt().coerceIn(1, PERCENT)
        }
    }
}
