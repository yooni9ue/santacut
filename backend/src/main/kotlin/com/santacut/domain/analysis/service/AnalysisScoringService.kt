package com.santacut.domain.analysis.service

import com.santacut.domain.analysis.model.AnalysisJob
import com.santacut.domain.analysis.model.AnalysisSubmission
import com.santacut.domain.analysis.model.ModelUsage
import com.santacut.domain.analysis.model.EarnedBadges
import com.santacut.domain.analysis.model.Score
import com.santacut.domain.analysis.model.ScoreAnswer
import com.santacut.domain.analysis.model.ScoredAssessment
import com.santacut.domain.analysis.model.ScoringAttempt
import com.santacut.domain.analysis.model.ScoringChunk
import com.santacut.domain.analysis.model.ScoringChunkAnswers
import com.santacut.domain.analysis.model.ScoringChunks
import com.santacut.domain.analysis.model.ScoringDigest
import com.santacut.domain.global.config.ScoreSamplingProperties
import com.santacut.domain.global.config.ScoreShrinkageProperties
import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import com.santacut.domain.global.model.Sha256
import com.santacut.domain.prompt.model.Prompt
import com.santacut.infrastructure.analysis.client.ScoreAnswerCalls
import com.santacut.infrastructure.analysis.client.ScoreClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.util.concurrent.Semaphore

@Service
class AnalysisScoringService(
    private val scoreClient: ScoreClient,
    private val objectMapper: ObjectMapper,
    shrinkageProperties: ScoreShrinkageProperties,
    samplingProperties: ScoreSamplingProperties,
) {
    private val shrinkage = shrinkageProperties.toShrinkage()
    private val samples = samplingProperties.samples
    private val chunkConcurrency = samplingProperties.chunkConcurrency
    private val logger = LoggerFactory.getLogger(AnalysisScoringService::class.java)

    /**
     * 모든 작업자를 합쳐 동시에 진행하는 채점 호출 자리. 작업자 3개가 구간을 4개씩 부르면 12개가 한꺼번에 나갈 수 있어 제공자 요청 한도에 닿는다.
     * 먼저 기다린 호출부터 자리를 받는다.
     */
    private val callSlots = Semaphore(samplingProperties.maxConcurrentCalls, FAIR_SLOTS)

    /** 동시에 부른 호출의 사용량을 하나씩 기록하게 하는 잠금. 같은 날 사용량 행을 두 호출이 함께 만들지 않게 한다. */
    private val usageLock = Any()

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

    /**
     * 작업 하나를 [prompt] 판으로 채점할 준비를 한다. 채점 입력을 만들고 같은 입력을 알아볼 해시를 낸다.
     * 본인 말이 너무 적으면 [ErrorCode.INSUFFICIENT_EVIDENCE]다. 로그에는 프롬프트 이름과 판 번호만 남긴다.
     */
    fun prepare(job: AnalysisJob, prompt: Prompt): ScoringAttempt {
        logger.info("분석 작업 {} 채점: 프롬프트 {} {}판", job.id, prompt.name, prompt.version)
        val digest = digest(job)
        return ScoringAttempt(job, prompt, digest, fingerprint(digest, prompt.version))
    }

    /** 호출 전 비용 확인에 쓰는 어림 사용량. 긴 대화는 모든 구간 호출의 합, 짧은 대화는 반복 호출 [samples]번의 합이다. */
    fun estimateUsage(attempt: ScoringAttempt): ModelUsage =
        ScoringChunks.of(attempt.digest).estimate(samples) { chunk -> scoreClient.estimateUsage(chunk, attempt.prompt.content) }

    /** 같은 채점 입력인지 가리는 해시. 모델 ID, 프롬프트 판, 점수 규칙 판, 반복 호출 수, 대화문 전체가 모두 같아야 같다. */
    fun fingerprint(digest: ScoringDigest, promptVersion: Int): String =
        Sha256.hex(
            scoreClient.cacheKey(digest, promptVersion) + RULE_VERSION_PREFIX + Score.RULE_VERSION + SAMPLES_PREFIX + samples,
        )

    /**
     * 대화문을 구간으로 나눠([ScoringChunks]) 구간마다 [systemPrompt]와 함께 AI에 묻는다.
     * 한 구간이면 [samples]번 동시에(가상 스레드, [ScoreAnswerCalls]) 묻고 항목별 중앙값으로 합친다.
     * 여러 구간이면 첫 구간을 먼저 묻고(뒤 호출이 시스템 프롬프트 캐시를 타게), 나머지를 한 번에 [chunkConcurrency]개까지 동시에 묻는다.
     * 비용 예약은 호출 전에 모든 구간 호출의 합으로 한 번에 했다. 호출마다 앱 전체 자리([callSlots])를 하나씩 잡는다.
     * 한 호출이 예외를 던지면 그 답만 버리고 남은 답의 과반으로 합친다.
     * 호출이 끝날 때마다 그 호출의 사용량을 [recordUsage]에 넘긴다. 다른 호출이 실패해도 이미 과금된 호출은 기록된다.
     * 기록이 실패하면 로그만 남기고 이미 과금된 답은 버리지 않는다.
     * 점수 검증과 합산은 [score]가 한다.
     */
    fun ask(digest: ScoringDigest, systemPrompt: String, recordUsage: (ModelUsage) -> Unit): ScoringChunkAnswers =
        ScoringChunks.of(digest).ask(samples, ScoreAnswerCalls::concurrently, ::askEachChunk) { chunk ->
            requestInSlot(chunk, systemPrompt).also { answer -> recordSafely(answer.usage, recordUsage) }
        }

    /** [attempt]의 채점 입력을 그 프롬프트 판으로 묻는다. 방법은 [ask]와 같다. */
    fun ask(attempt: ScoringAttempt, recordUsage: (ModelUsage) -> Unit): ScoringChunkAnswers =
        ask(attempt.digest, attempt.prompt.content, recordUsage)

    /** 앱 전체 채점 호출 자리([callSlots])를 하나 잡은 동안만 묻는다. */
    private fun requestInSlot(chunk: ScoringChunk, systemPrompt: String): ScoreAnswer {
        callSlots.acquire()
        try {
            return scoreClient.requestAnswer(chunk, systemPrompt)
        } finally {
            callSlots.release()
        }
    }

    private fun askEachChunk(chunks: List<ScoringChunk>, asker: (ScoringChunk) -> ScoreAnswer): List<ScoreAnswer> =
        ScoreAnswerCalls.firstThenEach(chunks, chunkConcurrency, asker)

    /** 사용량 기록 실패(일시적인 DB 오류 등)가 받은 답을 버리게 하지 않는다. 로그에는 예외 클래스 이름만 남긴다. */
    private fun recordSafely(usage: ModelUsage, recordUsage: (ModelUsage) -> Unit) {
        try {
            synchronized(usageLock) { recordUsage(usage) }
        } catch (exception: RuntimeException) {
            logger.warn("AI 사용량 기록 실패, 받은 답은 그대로 쓴다: {}", exception.javaClass.simpleName)
        }
    }

    /** 구간 답을 합치고 설정한 수축 규칙으로 점수를 낸다. */
    fun score(answers: ScoringChunkAnswers): Score = answers.toScore(shrinkage)

    /** 본인 말에서 받은 배지. 점수에 영향을 주지 않는다. */
    fun badges(digest: ScoringDigest): EarnedBadges = EarnedBadges.countedIn(digest.turns)

    /**
     * 구간 답을 점수, 한마디, 배지로 묶어 평가로 저장할 결과를 만든다. 점수를 낼 수 없으면 그 예외를 그대로 던진다.
     * 성공·실패 모두 사용량을 숫자로만 로그에 남긴다.
     */
    fun assess(attempt: ScoringAttempt, answers: ScoringChunkAnswers): ScoredAssessment {
        val score = scoreLogged(attempt, answers)
        return ScoredAssessment(score, attempt.prompt.version, answers.quoteOf(score), badges(attempt.digest))
    }

    private fun scoreLogged(attempt: ScoringAttempt, answers: ScoringChunkAnswers): Score {
        try {
            return score(answers).also { logUsage(attempt, answers, SCORED_OUTCOME) }
        } catch (exception: SantaCutException) {
            logUsage(attempt, answers, exception.errorCode.name)
            throw exception
        }
    }

    /** 숫자만 남긴다. 사용량은 모든 구간 호출의 합이고, 근거 보정은 범위 밖·겹친 근거 번호를 지운 수다. */
    private fun logUsage(attempt: ScoringAttempt, answers: ScoringChunkAnswers, outcome: String) {
        val usage = answers.usage()
        val digest = attempt.digest
        logger.info(
            "분석 작업 {} AI 사용량: 프롬프트 {}판, 메시지 {}개, 대화문 {}줄 {}자(뺀 줄 {}), 구간 {}개, 입력 {}(캐시 {}) 출력 {} 생각 {} 토큰, 종료 {}, 근거 보정 {}개, 결과 {}",
            attempt.job.id, attempt.prompt.version, digest.messageCount, digest.lineCount(), digest.turns.totalLength(), digest.omittedTurns, answers.size(),
            usage.inputTokens, usage.cachedInputTokens, usage.outputTokens, usage.thoughtTokens, usage.finishReason, answers.discardedEvidence(), outcome,
        )
    }

    companion object {
        private const val RULE_VERSION_PREFIX = "\nrule="
        private const val SAMPLES_PREFIX = "\nsamples="
        private const val SCORED_OUTCOME = "SCORED"
        private const val FAIR_SLOTS = true
    }
}
