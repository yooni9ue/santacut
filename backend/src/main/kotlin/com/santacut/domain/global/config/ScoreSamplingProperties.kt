package com.santacut.domain.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 짧은 대화(한 구간)를 몇 번 따로 채점해 항목별 중앙값을 낼지(self-consistency). 1이면 한 번만 묻는다.
 * 여러 구간으로 나뉘는 긴 대화는 이 값과 상관없이 구간마다 한 번 묻는다.
 *
 * @property samples 한 구간 대화의 채점 호출 수. 평가(`./gradlew eval`)로 골랐고 근거는 backend-design.md에 있다.
 * @property chunkConcurrency 여러 구간으로 나뉜 긴 대화에서 작업 하나가 동시에 부르는 구간 호출 수. 1이면 차례로 부른다.
 */
@ConfigurationProperties(prefix = "santacut.score.sampling")
data class ScoreSamplingProperties(
    val samples: Int = 3,
    val chunkConcurrency: Int = 4,
)
