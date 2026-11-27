package com.santacut.application.roast.result

import com.santacut.domain.analysis.model.GiftGrade
import com.santacut.domain.share.model.RecapDeckCard
import com.santacut.domain.share.model.RecapDeckKind

/**
 * 컷 표 카드. 여섯 구간 세로 표에서 내 칸만 불을 켜고, 구간 고정 문구를 싣는다. 값은 모두 서버 것이다.
 *
 * @property grade 내 구간 이름
 * @property gradeCode 내 구간 코드
 * @property bandDetail 구간 고정 문구(서버 상수, 예: "바지 걸렸는데 통과")
 * @property bandPosition 내 칸의 자리(0부터, 0이 가장 높은 구간)
 * @property bands 여섯 칸
 */
data class RecapCutlineCard(
    override val index: Int,
    val grade: String,
    val gradeCode: String,
    val bandDetail: String,
    val bandPosition: Int,
    val bands: RecapBandResults,
) : RecapCard {
    override val kind: RecapCardKind = RecapCardKind.CUTLINE

    override fun at(index: Int): RecapCard = copy(index = index)

    override fun toDeckCard(top: RecapTopResult, scoreText: String): RecapDeckCard = RecapDeckCard(
        index, RecapDeckKind.CUTLINE, scoreText, top.rank, top.topPercent, grade = grade, bandDetail = bandDetail, bands = bands.toDeckBands(),
    )

    companion object {
        fun of(grade: GiftGrade): RecapCutlineCard =
            RecapCutlineCard(0, grade.label, grade.name, grade.detail, grade.ordinal, RecapBandResults.of(grade))
    }
}
