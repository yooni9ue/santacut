package com.santacut.domain.dossier.model

import com.santacut.domain.analysis.model.Assessment
import com.santacut.domain.analysis.model.GiftVerdict
import com.santacut.domain.analysis.model.ScoreComponents
import com.santacut.domain.analysis.model.ScoreDimension
import com.santacut.domain.analysis.model.ScoreVerdict
import com.santacut.domain.global.model.SeoulTime
import java.time.Instant

/**
 * 조서가 함께 보여 주는 기존 결과. 선물 판정, 항목 점수, 순위, 도착 시각, 지역은 결과 화면과 같은 평가·스냅숏에서 온다.
 *
 * @property assessmentId 템플릿 선택 시드로 쓰는 결과(평가) ID
 * @property rank 전국 순위. 첫 순위 계산 전이면 null이다.
 * @property eta 배송 도착 예정 시각. 첫 순위 계산 전이면 null이다.
 * @property regionName 참가자 지역 이름. 없으면 null이다.
 * @property caseYear 조서 번호에 쓰는 해(평가를 만든 해, 한국 시각)
 */
class DossierSubject(
    val assessmentId: String,
    private val scoreHundredths: Int,
    private val dimensionScores: ScoreComponents,
    val rank: Int?,
    val eta: Instant?,
    val regionName: String?,
    val caseYear: Int,
) {
    /** 판결 장 제목과 문구 슬롯에 쓰는 「팩폭 산타」 선물 판정. 기존 결과 API의 점수 판정 제목은 쓰지 않는다. */
    val gift: GiftVerdict = GiftVerdict(scoreHundredths)

    /** 판결 제목(도장과 등급). 예: "선물 배송 확정 · 턱걸이 통과". */
    val verdictTitle: String = gift.headline()

    fun band(): VerdictBand = VerdictBand.of(ScoreVerdict.of(scoreHundredths))

    /** 4개 항목 중 점수가 가장 높은 항목. 같으면 가중치가 큰 항목이다. */
    fun topDimension(): ScoreDimension = dimensionScores.rankedDimensions()[0]

    companion object {
        fun of(assessment: Assessment, rank: Int?, eta: Instant?, regionName: String?): DossierSubject = DossierSubject(
            assessment.id, assessment.scoreHundredths, assessment.dimensionScores(), rank, eta, regionName,
            assessment.createdAt.atZone(SeoulTime.ZONE).year,
        )
    }
}
