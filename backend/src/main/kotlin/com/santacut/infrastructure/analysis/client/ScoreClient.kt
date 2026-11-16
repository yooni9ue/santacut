package com.santacut.infrastructure.analysis.client

import com.santacut.domain.analysis.model.ModelUsage
import com.santacut.domain.analysis.model.ScoreAnswer
import com.santacut.domain.analysis.model.ScoringDigest
import com.santacut.infrastructure.global.config.ScoreModelProperties
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.google.genai.metadata.GoogleGenAiUsage
import org.springframework.stereotype.Component
import tools.jackson.core.JacksonException
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

/**
 * Gemini 모델(Google GenAI)에 항목별 점수와 근거 번호를 묻고, 응답 JSON을 [ScoreAnswer]로 읽는다.
 * 시스템 프롬프트는 호출하는 쪽이 DB에서 읽어 넘긴다. 점수 검증과 합산은 도메인 모델이 한다.
 * 사용자 메시지에는 이벤트 원문 전체가 아니라 항목별 낱말 신호와 작은 표본만 담는다([ScoreModelInput]).
 * 응답 메타데이터의 토큰 사용량(생각 토큰 포함)과 종료 사유는 [ModelUsage]로 넘긴다. 프롬프트, 사용자 메시지, 모델 응답은 로그에 남기지 않는다.
 * JSON 응답 모드를 켜 두지만, 모델이 JSON을 코드 블록으로 감싸 보내도 읽도록 첫 `{`부터 마지막 `}`까지만 읽는다.
 */
@Component
class ScoreClient(
    private val objectMapper: ObjectMapper,
    chatClientBuilder: ChatClient.Builder,
    private val scoreModelProperties: ScoreModelProperties,
) {
    private val chatClient = chatClientBuilder.build()

    fun isEnabled(): Boolean = scoreModelProperties.model.isNotBlank()

    /** 같은 입력인지 가리는 키. 모델 ID, 프롬프트 판, 사용자 메시지를 잇는다. 본문이 들어 있으므로 해시로만 저장한다. */
    fun cacheKey(digest: ScoringDigest, promptVersion: Int): String =
        "${scoreModelProperties.model}$KEY_SEPARATOR$promptVersion$KEY_SEPARATOR${ScoreModelInput.from(digest).toText()}"

    /**
     * [systemPrompt]를 시스템 메시지로, [digest]의 신호와 번호 붙인 표본을 사용자 메시지로 보낸다.
     * 제공자가 권한 거부, 요청 한도, 서버 오류, 시간 초과로 막으면 [ScoreAnswer.providerUnavailable]이다. 그 밖의 호출 오류는 그대로 던진다.
     */
    fun requestAnswer(digest: ScoringDigest, systemPrompt: String): ScoreAnswer =
        try {
            answerOf(chatClient.prompt().system(systemPrompt).user(ScoreModelInput.from(digest).toText()).call().chatResponse())
        } catch (exception: RuntimeException) {
            if (!ExceptionCauses.of(exception).isProviderUnavailable()) throw exception
            ScoreAnswer.providerUnavailable()
        }

    private fun answerOf(response: ChatResponse?): ScoreAnswer {
        val usage = usageOf(response)
        val content = response?.result?.output?.text
        if (content.isNullOrBlank() || content.length > MAX_RESPONSE_LENGTH) return ScoreAnswer.unreadable(usage)
        val json = jsonObjectIn(content) ?: return ScoreAnswer.unreadable(usage)
        val tree = readTree(json) ?: return ScoreAnswer.unreadable(usage)
        return toAnswer(tree, usage)
    }

    private fun usageOf(response: ChatResponse?): ModelUsage {
        val usage = response?.metadata?.usage
        val finishReason = response?.result?.metadata?.finishReason.orEmpty()
        val thoughtTokens = (usage as? GoogleGenAiUsage)?.thoughtsTokenCount ?: 0
        return ModelUsage(
            (usage?.promptTokens ?: 0).toLong(),
            (usage?.completionTokens ?: 0).toLong(),
            finishReason,
            thoughtTokens.toLong(),
        )
    }

    /** 응답에서 첫 `{`부터 마지막 `}`까지. 중괄호 쌍이 없으면 null이다. */
    private fun jsonObjectIn(content: String): String? {
        val start = content.indexOf(JSON_OBJECT_START)
        val end = content.lastIndexOf(JSON_OBJECT_END)
        return if (start in 0..<end) content.substring(start, end + 1) else null
    }

    /** 출력 한도에 걸려 잘린 JSON처럼 읽을 수 없으면 null이다. */
    private fun readTree(json: String): JsonNode? =
        try {
            objectMapper.readTree(json)
        } catch (exception: JacksonException) {
            null
        }

    private fun toAnswer(response: JsonNode, usage: ModelUsage): ScoreAnswer {
        if (!response.path(SUFFICIENT_FIELD).asBoolean(false)) return ScoreAnswer.insufficient(usage)
        val scores = response.path(SCORES_FIELD)
        val evidence = response.path(EVIDENCE_FIELD)
        return ScoreAnswer.of(
            ScoreDimensionNodes.of(scores).toScoreValues(),
            ScoreDimensionNodes.of(evidence).toEvidenceIndices(),
            "scores=${scores.nodeType},evidence=${evidence.nodeType}",
            usage,
        )
    }

    companion object {
        private const val MAX_RESPONSE_LENGTH = 100_000
        private const val SUFFICIENT_FIELD = "sufficient"
        private const val SCORES_FIELD = "scores"
        private const val EVIDENCE_FIELD = "evidence"
        private const val JSON_OBJECT_START = '{'
        private const val JSON_OBJECT_END = '}'
        private const val KEY_SEPARATOR = "\n"
    }
}
