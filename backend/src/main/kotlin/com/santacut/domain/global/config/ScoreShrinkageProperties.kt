package com.santacut.domain.global.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.math.BigDecimal

/**
 * 항목 점수 수축(shrinkage) 설정. 항목 점수 x를 근거 무게 w만큼 믿고 나머지는 사전 평균 m 쪽으로 당긴다.
 * x' = (C·m + w·x) / (C + w). w는 그 항목에서 행동을 보일 기회 수(n)와 본인 턴 수로 정한다.
 * 사전 평균 m은 설정이 아니라 중립 점수(`DimensionScores.NEUTRAL_SCORE`)다.
 *
 * @property strength 사전 평균의 무게 C. 평가(`./gradlew eval`)로 골랐고 근거는 backend-design.md에 있다.
 * @property fullSelfTurns 본인 턴이 이만큼 있으면 근거 무게를 깎지 않는다. 이보다 짧은 대화만 덜 믿는다.
 * @property personalPrior 근거 없는 항목에 중립 점수와 본인의 다른 항목 평균을 섞은 값을 준다(backend-design.md §14.12).
 */
@ConfigurationProperties(prefix = "santacut.score.shrinkage")
data class ScoreShrinkageProperties(
    val strength: BigDecimal = BigDecimal("0.25"),
    val fullSelfTurns: Int = 5,
    val personalPrior: Boolean = true,
)
