package com.santacut.domain.ranking.model

import com.santacut.domain.analysis.model.Archetype
import com.santacut.domain.analysis.model.ArchetypeCounts
import com.santacut.domain.analysis.model.Assessment
import com.santacut.domain.analysis.model.Assessments
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * 가장 최근 순위 계산 때 참가자별 최신 평가로 센 장난기 기준값과 유형 분포. 행은 [LATEST_ID] 하나뿐이다.
 * 결과 화면이 요청마다 평가 전체를 읽지 않도록 순위를 다시 계산할 때 함께 저장한다.
 *
 * @property playfulnessSplit 장난기 높낮이를 가르는 기준값([Assessments.playfulnessSplit])
 */
@Entity
@Table(name = "result_populations")
class ResultPopulation(
    id: Int,
    playfulnessSplit: Int,
    archetypeCounts: String,
) {
    @Id
    @Column(name = "id")
    var id: Int = id
        protected set

    @Column(name = "playfulness_split", nullable = false)
    var playfulnessSplit: Int = playfulnessSplit
        protected set

    @Column(name = "archetype_counts", nullable = false)
    var archetypeCounts: String = archetypeCounts
        protected set

    /** [assessment]의 유형. 장난기 높낮이는 저장한 기준값으로 가른다. */
    fun archetypeOf(assessment: Assessment): Archetype = assessment.archetype(playfulnessSplit)

    /** 유형이 [archetype]인 참가자 비율(%). 참가자가 [ArchetypeCounts.MIN_PARTICIPANTS_FOR_SHARE]명보다 적으면 null이다. */
    fun sharePercentOf(archetype: Archetype): Int? = ArchetypeCounts.fromColumn(archetypeCounts).sharePercentOf(archetype)

    companion object {
        const val LATEST_ID = 1

        /** 평가가 하나도 없을 때의 분포. 기준값은 기본값이고 유형 비율은 모두 null이다. */
        val EMPTY: ResultPopulation get() = of(Assessments(emptyList()))

        /** 참가자별 최신 평가 [population]으로 기준값과 유형 분포를 센다. */
        fun of(population: Assessments): ResultPopulation {
            val split = population.playfulnessSplit()
            return ResultPopulation(LATEST_ID, split, population.archetypeCounts(split).toColumn())
        }
    }
}
