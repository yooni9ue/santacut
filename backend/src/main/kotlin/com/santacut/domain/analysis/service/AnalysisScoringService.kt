package com.santacut.domain.analysis.service

import com.santacut.domain.analysis.model.AnalysisJob
import com.santacut.domain.analysis.model.AnalysisSubmission
import com.santacut.domain.analysis.model.ModelUsage
import com.santacut.domain.analysis.model.EarnedBadges
import com.santacut.domain.analysis.model.Score
import com.santacut.domain.analysis.model.ScoreShrinkage
import com.santacut.domain.analysis.model.ScoringChunkAnswers
import com.santacut.domain.analysis.model.ScoringChunks
import com.santacut.domain.analysis.model.ScoringDigest
import com.santacut.domain.global.config.ScoreShrinkageProperties
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
    shrinkageProperties: ScoreShrinkageProperties,
) {
    private val shrinkage = ScoreShrinkage(shrinkageProperties.strength, shrinkageProperties.fullSelfTurns)

    fun isEnabled(): Boolean = scoreClient.isEnabled()

    fun verifyEnabled() {
        if (!isEnabled()) throw SantaCutException(ErrorCode.AI_NOT_CONFIGURED)
    }

    /**
     * 작업에 저장한 제출 내용으로 채점 입력(정규화한 대화문)을 만든다.
     * 본인 말이 너무 적으면 AI를 부르지 않고 [ErrorCode.INSUFFICIENT_EVIDENCE]다.
     */
    fun digest(job: AnalysisJob): ScoringDigest =
        objectMapper.readValue(job.requirePayload(), AnalysisSubmission::class.java).toScoringDigest().verifyEnough()

    /** 호출 전 비용 확인에 쓰는 어림 사용량. 긴 대화는 모든 구간 호출의 합이다. */
    fun estimateUsage(digest: ScoringDigest, systemPrompt: String): ModelUsage =
        ScoringChunks.of(digest).estimate { chunk -> scoreClient.estimateUsage(chunk, systemPrompt) }

    /** 같은 채점 입력인지 가리는 해시. 모델 ID, 프롬프트 판, 점수 규칙 판, 대화문 전체가 모두 같아야 같다. */
    fun fingerprint(digest: ScoringDigest, promptVersion: Int): String =
        Sha256.hex(scoreClient.cacheKey(digest, promptVersion) + RULE_VERSION_PREFIX + Score.RULE_VERSION)

    /**
     * 대화문을 구간으로 나눠([ScoringChunks]) 구간마다 [systemPrompt]와 함께 AI에 묻는다.
     * 구간 호출이 끝날 때마다 그 호출의 사용량을 [recordUsage]에 넘긴다. 뒤 구간이 실패해도 이미 과금된 호출은 기록된다.
     * 점수 검증과 합산은 [score]가 한다.
     */
    fun ask(digest: ScoringDigest, systemPrompt: String, recordUsage: (ModelUsage) -> Unit): ScoringChunkAnswers =
        ScoringChunks.of(digest).ask { chunk ->
            scoreClient.requestAnswer(chunk, systemPrompt).also { answer -> recordUsage(answer.usage) }
        }

    /** 구간 답을 합치고 설정한 수축 규칙으로 점수를 낸다. */
    fun score(answers: ScoringChunkAnswers): Score = answers.toScore(shrinkage)

    /** 본인 말에서 받은 배지. 점수에 영향을 주지 않는다. */
    fun badges(digest: ScoringDigest): EarnedBadges = EarnedBadges.countedIn(digest.turns)

    companion object {
        private const val RULE_VERSION_PREFIX = "\nrule="
    }
}
