package com.santacut.domain.analysis.service

import com.santacut.domain.analysis.model.AnalysisJob
import com.santacut.domain.analysis.model.AnalysisSubmission
import com.santacut.domain.analysis.model.ScoreAnswer
import com.santacut.domain.analysis.model.ScoringDigest
import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import com.santacut.domain.global.model.Sha256
import com.santacut.infrastructure.analysis.client.ScoreClient
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper

@Service
class AnalysisScoringService(
    private val scoreClient: ScoreClient,
    private val objectMapper: ObjectMapper,
) {
    fun isEnabled(): Boolean = scoreClient.isEnabled()

    fun verifyEnabled() {
        if (!isEnabled()) throw SantaCutException(ErrorCode.AI_NOT_CONFIGURED)
    }

    /**
     * 작업에 저장한 제출 내용으로 채점 입력(통계, 항목별 낱말 신호, 표본)을 만든다.
     * 표본이 너무 적으면 AI를 부르지 않고 [ErrorCode.INSUFFICIENT_EVIDENCE]다.
     */
    fun digest(job: AnalysisJob): ScoringDigest =
        objectMapper.readValue(job.requirePayload(), AnalysisSubmission::class.java).toScoringDigest().verifySampled()

    /** 같은 채점 입력인지 가리는 해시. 모델 ID, 프롬프트 판, AI 사용자 메시지가 모두 같아야 같다. */
    fun fingerprint(digest: ScoringDigest, promptVersion: Int): String = Sha256.hex(scoreClient.cacheKey(digest, promptVersion))

    /** [systemPrompt]와 [digest]를 AI에 보내 답과 토큰 사용량을 받는다. 점수 검증은 [ScoreAnswer.toScore]가 한다. */
    fun ask(digest: ScoringDigest, systemPrompt: String): ScoreAnswer = scoreClient.requestAnswer(digest, systemPrompt)
}
