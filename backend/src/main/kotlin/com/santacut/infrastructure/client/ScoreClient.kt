package com.santacut.infrastructure.client

import com.santacut.domain.model.AnalysisSubmission
import com.santacut.domain.model.ScoreAnswer
import com.santacut.infrastructure.config.ScoreModelProperties
import org.springframework.ai.chat.client.ChatClient
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

/**
 * Ollama 모델에 항목별 점수와 근거 번호를 묻고, 응답 JSON을 [ScoreAnswer]로 읽는다.
 * 점수 검증과 합산은 도메인 모델이 한다.
 */
@Component
class ScoreClient(
    private val objectMapper: ObjectMapper,
    chatClientBuilder: ChatClient.Builder,
    private val scoreModelProperties: ScoreModelProperties,
) {
    private val chatClient = chatClientBuilder.build()
    private val systemPrompt: String by lazy {
        ClassPathResource(PROMPT_PATH).inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    fun isEnabled(): Boolean = scoreModelProperties.model.isNotBlank()

    fun requestAnswer(submission: AnalysisSubmission): ScoreAnswer {
        val userMessage = objectMapper.writeValueAsString(ScoreModelInput.from(submission))
        val content = chatClient.prompt().system(systemPrompt).user(userMessage).call().content()
        if (content.isNullOrBlank() || content.length > MAX_RESPONSE_LENGTH) return ScoreAnswer.unreadable()
        return toAnswer(objectMapper.readTree(content))
    }

    private fun toAnswer(response: JsonNode): ScoreAnswer {
        if (!response.path(SUFFICIENT_FIELD).asBoolean(false)) return ScoreAnswer.insufficient()
        val scores = response.path(SCORES_FIELD)
        val evidence = response.path(EVIDENCE_FIELD)
        return ScoreAnswer.of(
            ScoreDimensionNodes.of(scores).toScoreValues(),
            ScoreDimensionNodes.of(evidence).toEvidenceIndices(),
            "scores=${scores.nodeType},evidence=${evidence.nodeType}",
        )
    }

    companion object {
        private const val PROMPT_PATH = "prompts/score-v1.txt"
        private const val MAX_RESPONSE_LENGTH = 100_000
        private const val SUFFICIENT_FIELD = "sufficient"
        private const val SCORES_FIELD = "scores"
        private const val EVIDENCE_FIELD = "evidence"
    }
}
