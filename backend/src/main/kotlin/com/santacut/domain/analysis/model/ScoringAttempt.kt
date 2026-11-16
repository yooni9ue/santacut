package com.santacut.domain.analysis.model

import com.santacut.domain.prompt.model.Prompt

/**
 * 리스를 쥔 분석 작업 하나를 한 번 채점하는 데 함께 쓰는 값.
 *
 * @property lease 작업을 쥔 리스 토큰. 결과를 기록할 때 아직 쥐고 있는지 확인한다.
 * @property prompt 이번 채점에 쓴 채점 프롬프트 판
 * @property digest 정규화한 채점 입력
 * @property digestHash 같은 채점 입력의 재제출을 알아보는 해시
 */
data class ScoringAttempt(
    val job: AnalysisJob,
    val lease: String,
    val prompt: Prompt,
    val digest: ScoringDigest,
    val digestHash: String,
)
