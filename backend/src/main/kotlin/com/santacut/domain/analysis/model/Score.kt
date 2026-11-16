package com.santacut.domain.analysis.model

import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import java.math.BigDecimal

/**
 * 항목별 점수와 가중 합계. 합계는 100분의 1점 단위 정수다.
 * 검증과 가중 합산은 [ScoreComponents]가 한다.
 *
 * @property playfulness 순위에 쓰지 않는 장난기 성향(0~100). 모르면 null이다.
 * @property rawMeans 수축 전 항목별 평균(소수 둘째 자리 반올림). 순위에는 쓰지 않고 유형을 고를 때만 쓴다.
 */
class Score private constructor(
    val hundredths: Int,
    val components: ScoreComponents,
    val lowConfidence: LowConfidenceDimensions,
    val playfulness: Int?,
    val rawMeans: ScoreComponents,
) {
    fun verdict(): ScoreVerdict = ScoreVerdict.of(hundredths)

    companion object {
        /**
         * 점수 규칙 판. 평가 행에 함께 저장한다.
         * 1판(값 없음)은 한 번 호출한 항목 점수를 그대로 가중 합산했다. 2판은 구간별 채점을 합치고 항목 점수를 수축한다(C=0.5, 본인 턴 20).
         * 3판은 수축을 약하게 했다(C=0.25, 본인 턴 5). 4판은 짧은 대화를 여러 번 따로 물어 항목별 중앙값을 낸다.
         * 5판은 근거 없는 항목에 중립 점수 대신 중립 점수와 본인의 다른 항목 평균을 함께 평균한 값을 준다.
         * 6판은 긴 대화를 더 잘게 나눈다(구간 6,700자, 최대 6구간).
         * 판이 바뀌면 같은 채점 입력도 다시 채점한다.
         */
        const val RULE_VERSION = 6

        /**
         * 수축한 정밀 항목 점수 [precise]로 합계를 낸다. 합계는 정밀 값의 가중 합을 한 번만 반올림하고,
         * 저장·표시용 항목 점수는 소수 둘째 자리로 반올림한다. 수축 전 평균 [rawMeans]를 모르면 [precise]로 본다.
         */
        fun shrunk(
            precise: List<BigDecimal>,
            lowConfidence: LowConfidenceDimensions,
            playfulness: Int?,
            rawMeans: List<BigDecimal> = precise,
        ): Score {
            val scoreComponents = ScoreComponents(precise)
            if (!scoreComponents.isWithinBounds()) throw SantaCutException(ErrorCode.AI_SCORE_OUT_OF_RANGE)
            val rounded = scoreComponents.rounded()
            return Score(scoreComponents.weightedHundredths(), rounded, lowConfidence, playfulness, ScoreComponents(rawMeans).rounded())
        }
    }
}
