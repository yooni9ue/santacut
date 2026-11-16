package com.santacut.infrastructure.analysis.client

import com.santacut.domain.analysis.model.AnalysisSubmission
import com.santacut.domain.analysis.model.ScoreAnswer
import com.santacut.infrastructure.global.config.ScoreModelProperties
import org.springframework.ai.chat.client.ChatClient
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

/**
 * Bedrock 모델(Converse API)에 항목별 점수와 근거 번호를 묻고, 응답 JSON을 [ScoreAnswer]로 읽는다.
 * 점수 검증과 합산은 도메인 모델이 한다.
 * Converse API에는 JSON 강제 모드가 없어 모델이 JSON을 코드 블록으로 감싸 보낼 수 있다. 그래서 첫 `{`부터 마지막 `}`까지만 읽는다.
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
        val json = jsonObjectIn(content) ?: return ScoreAnswer.unreadable()
        return toAnswer(objectMapper.readTree(json))
    }

    /** 응답에서 첫 `{`부터 마지막 `}`까지. 중괄호 쌍이 없으면 null이다. */
    private fun jsonObjectIn(content: String): String? {
        val start = content.indexOf(JSON_OBJECT_START)
        val end = content.lastIndexOf(JSON_OBJECT_END)
        return if (start in 0..<end) content.substring(start, end + 1) else null
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
        private const val JSON_OBJECT_START = '{'
        private const val JSON_OBJECT_END = '}'
    }
}
