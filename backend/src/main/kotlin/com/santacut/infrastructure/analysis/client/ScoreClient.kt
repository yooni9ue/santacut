package com.santacut.infrastructure.analysis.client

import com.santacut.domain.analysis.model.ModelUsage
import com.santacut.domain.analysis.model.DimensionOpportunities
import com.santacut.domain.analysis.model.ScoreAnswer
import com.santacut.domain.analysis.model.ScoreDimension
import com.santacut.domain.analysis.model.ScoringChunk
import com.santacut.domain.analysis.model.ScoringDigest
import com.santacut.infrastructure.global.config.ScoreModelProperties
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.google.genai.GoogleGenAiChatOptions
import org.springframework.ai.google.genai.metadata.GoogleGenAiUsage
import org.springframework.stereotype.Component
import tools.jackson.core.JacksonException
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

/**
 * Gemini 모델(Google GenAI)에 항목별 점수와 근거 번호를 묻고, 응답 JSON을 [ScoreAnswer]로 읽는다.
 * 시스템 프롬프트는 호출하는 쪽이 DB에서 읽어 넘긴다. 점수 검증과 합산은 도메인 모델이 한다.
 * 사용자 메시지에는 분석 기간의 대화를 정규화한 대화문의 한 구간([ScoringChunk])을 담는다([ScoreModelInput]).
 * 응답은 항목 키마다 `{"e":[근거 줄],"n":기회 수,"s":점수}`와 `playfulness`를 담은 6판 형식이다.
 * 예전 형식(`scores`·`evidence` 객체)도 읽고, 그때 기회 수는 모르는 값([DimensionOpportunities.UNKNOWN])이 된다.
 * 응답 메타데이터의 토큰 사용량(생각 토큰 포함)과 종료 사유는 [ModelUsage]로 넘긴다. 프롬프트, 사용자 메시지, 모델 응답은 로그에 남기지 않는다.
 * 응답 형태는 구조화 출력([ScoreResponseSchema])으로 강제한다. 제약 디코딩이라 코드 블록으로 감싸 오지 않으므로 응답 본문을 그대로 JSON으로 읽는다.
 * 잘리거나 깨진 JSON은 읽을 수 없는 답([ScoreAnswer.unreadable])이고, 값의 범위 검증은 도메인 모델이 한다.
 */
@Component
class ScoreClient(
    private val objectMapper: ObjectMapper,
    chatClientBuilder: ChatClient.Builder,
    private val scoreModelProperties: ScoreModelProperties,
) {
    private val chatClient = chatClientBuilder.build()
    private val logger = LoggerFactory.getLogger(ScoreClient::class.java)

    fun isEnabled(): Boolean = scoreModelProperties.model.isNotBlank()

    /** 같은 입력인지 가리는 키. 모델 ID, 프롬프트 판, 사용자 메시지를 잇는다. 본문이 들어 있으므로 해시로만 저장한다. */
    fun cacheKey(digest: ScoringDigest, promptVersion: Int): String =
        "${scoreModelProperties.model}$KEY_SEPARATOR$promptVersion$KEY_SEPARATOR${ScoreModelInput.from(ScoringChunk.whole(digest)).toText()}"

    /**
     * 호출 한 번의 어림 사용량. 입력은 시스템 프롬프트와 사용자 메시지 글자 수로([estimatedTokens]), 출력은 출력 토큰 상한으로 잡는다.
     * 비용 가드가 호출 전에 이 값으로 한도를 넘지 않을지 확인한다.
     */
    fun estimateUsage(chunk: ScoringChunk, systemPrompt: String): ModelUsage = ModelUsage(
        estimatedTokens(systemPrompt.length + ScoreModelInput.from(chunk).toText().length),
        scoreModelProperties.maxOutputTokens.toLong(),
        ESTIMATED_FINISH_REASON,
    )

    /**
     * [systemPrompt]를 시스템 메시지로, [chunk]의 번호 붙인 대화문을 사용자 메시지로 보낸다.
     * 제공자가 요청 한도, 서버 오류, 시간 초과, 설정 오류(API 키·권한·모델 이름)로 막으면 [ScoreAnswer.providerUnavailable]이다.
     * 그 밖의 호출 오류는 그대로 던진다.
     */
    fun requestAnswer(chunk: ScoringChunk, systemPrompt: String): ScoreAnswer =
        try {
            answerOf(
                chatClient.prompt().options(GoogleGenAiChatOptions.builder().responseSchema(ScoreResponseSchema.JSON))
                    .system(systemPrompt).user(ScoreModelInput.from(chunk).toText()).call().chatResponse(),
            )
        } catch (exception: RuntimeException) {
            unavailableOrThrow(ExceptionCauses.of(exception), exception)
        }

    /** 설정 오류는 운영자가 고칠 때까지 모든 작업이 멈추므로 오류 로그로 크게 남긴다. 제공자 메시지는 남기지 않는다. */
    private fun unavailableOrThrow(causes: ExceptionCauses, exception: RuntimeException): ScoreAnswer {
        causes.misconfiguredCode()?.let { code ->
            logger.error("AI 제공자 설정 오류(HTTP {}): API 키, 권한, 모델 이름을 확인해야 한다. 작업은 대기열에 남는다", code)
        }
        if (!causes.isProviderUnavailable()) throw exception
        return ScoreAnswer.providerUnavailable()
    }

    private fun answerOf(response: ChatResponse?): ScoreAnswer {
        val usage = usageOf(response)
        val content = response?.result?.output?.text
        if (content.isNullOrBlank() || content.length > MAX_RESPONSE_LENGTH) return ScoreAnswer.unreadable(usage)
        val tree = readTree(content) ?: return ScoreAnswer.unreadable(usage)
        return toAnswer(tree, usage)
    }

    private fun usageOf(response: ChatResponse?): ModelUsage {
        val usage = response?.metadata?.usage
        val finishReason = response?.result?.metadata?.finishReason.orEmpty()
        val genAiUsage = usage as? GoogleGenAiUsage
        return ModelUsage(
            (usage?.promptTokens ?: 0).toLong(),
            (usage?.completionTokens ?: 0).toLong(),
            finishReason,
            (genAiUsage?.thoughtsTokenCount ?: 0).toLong(),
            (genAiUsage?.cachedContentTokenCount ?: 0).toLong(),
        )
    }

    /** 출력 한도에 걸려 잘린 JSON처럼 읽을 수 없거나 JSON 객체가 아니면 null이다. */
    private fun readTree(json: String): JsonNode? =
        try {
            objectMapper.readTree(json).takeIf { it.isObject }
        } catch (exception: JacksonException) {
            null
        }

    private fun toAnswer(response: JsonNode, usage: ModelUsage): ScoreAnswer {
        if (!response.path(SUFFICIENT_FIELD).asBoolean(false)) return ScoreAnswer.insufficient(usage)
        if (response.has(LEGACY_SCORES_FIELD)) return legacyAnswer(response, usage)
        val dimensions = ScoreDimensionNodes.of(response)
        return ScoreAnswer.of(
            dimensions.field(SCORE_FIELD).toScoreValues(),
            dimensions.field(EVIDENCE_FIELD).toEvidenceIndices(),
            "dimensions=${response.path(ScoreDimension.CARE_EMPATHY.key).nodeType}",
            usage,
            dimensions.field(OPPORTUNITY_FIELD).toOpportunities(),
            playfulnessOf(response.path(PLAYFULNESS_FIELD)),
        )
    }

    /** 5판까지의 형식. 기회 수와 장난기가 없다. */
    private fun legacyAnswer(response: JsonNode, usage: ModelUsage): ScoreAnswer {
        val scores = response.path(LEGACY_SCORES_FIELD)
        val evidence = response.path(LEGACY_EVIDENCE_FIELD)
        return ScoreAnswer.of(
            ScoreDimensionNodes.of(scores).toScoreValues(),
            ScoreDimensionNodes.of(evidence).toEvidenceIndices(),
            "scores=${scores.nodeType},evidence=${evidence.nodeType}",
            usage,
        )
    }

    /** 숫자가 아니면 null이다. 범위 확인은 [ScoreAnswer.of]가 한다. */
    private fun playfulnessOf(node: JsonNode): Int? = if (node.isNumber) node.asInt() else null

    companion object {
        private const val MAX_RESPONSE_LENGTH = 100_000
        private const val SUFFICIENT_FIELD = "sufficient"
        private const val LEGACY_SCORES_FIELD = "scores"
        private const val LEGACY_EVIDENCE_FIELD = "evidence"
        private const val SCORE_FIELD = "s"
        private const val EVIDENCE_FIELD = "e"
        private const val OPPORTUNITY_FIELD = "n"
        private const val PLAYFULNESS_FIELD = "playfulness"
        private const val KEY_SEPARATOR = "\n"
        private const val ESTIMATED_FINISH_REASON = "ESTIMATE"

        /** 어림 토큰 수 = 글자 수 × 3 / 4(올림). 한국어 대화문 실측(2,000개 대화 53,132자 → 37,838토큰)보다 조금 넉넉하게 잡았다. */
        private const val TOKENS_PER_CHARS_NUMERATOR = 3L
        private const val TOKENS_PER_CHARS_DENOMINATOR = 4L

        /** 글자 [chars]개를 보낼 때의 어림 입력 토큰 수. */
        fun estimatedTokens(chars: Int): Long =
            (chars * TOKENS_PER_CHARS_NUMERATOR + TOKENS_PER_CHARS_DENOMINATOR - 1) / TOKENS_PER_CHARS_DENOMINATOR
    }
}
