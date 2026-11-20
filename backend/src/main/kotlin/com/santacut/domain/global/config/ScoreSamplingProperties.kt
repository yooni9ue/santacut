package com.santacut.domain.global.config

import com.santacut.domain.analysis.model.ScoringChunks
import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 짧은 대화(한 구간)를 몇 번 따로 채점해 항목별 중앙값을 낼지(self-consistency). 1이면 한 번만 묻는다.
 * 여러 구간으로 나뉘는 긴 대화는 이 값과 상관없이 구간마다 한 번 묻는다.
 *
 * @property samples 한 구간 대화의 채점 호출 수. 평가(`./gradlew eval`)로 골랐고 근거는 backend-design.md에 있다.
 * @property chunkConcurrency 여러 구간으로 나뉜 긴 대화에서 작업 하나가 동시에 부르는 구간 호출 수. 1이면 차례로 부른다.
 *   1부터 구간 수 상한([ScoringChunks.MAX_CHUNKS])까지만 받는다.
 * @property maxConcurrentCalls 모든 작업자를 합쳐 동시에 진행하는 채점 호출 수 상한. 작업자 수 × 구간 동시 호출 수가 제공자 요청 한도를
 *   넘지 않게 묶는다. 자리를 기다리는 시간도 작업 리스 안에서 흐르므로 작업자 수보다 작게 두지 않는다.
 */
@ConfigurationProperties(prefix = "santacut.score.sampling")
data class ScoreSamplingProperties(
    val samples: Int = 3,
    val chunkConcurrency: Int = 4,
    val maxConcurrentCalls: Int = 6,
) {
    init {
        require(chunkConcurrency in 1..ScoringChunks.MAX_CHUNKS) {
            "santacut.score.sampling.chunk-concurrency는 1~${ScoringChunks.MAX_CHUNKS}이어야 한다: $chunkConcurrency"
        }
        require(maxConcurrentCalls >= 1) { "santacut.score.sampling.max-concurrent-calls는 1 이상이어야 한다: $maxConcurrentCalls" }
    }
}
