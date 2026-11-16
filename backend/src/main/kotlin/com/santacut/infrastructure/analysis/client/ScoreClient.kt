package com.santacut.infrastructure.analysis.client

import com.santacut.domain.analysis.model.AnalysisSubmission
import com.santacut.domain.analysis.model.ScoreAnswer
import com.santacut.infrastructure.global.config.ScoreModelProperties
import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

/**
 * Bedrock 모델(Converse API)에 항목별 점수와 근거 번호를 묻고, 응답 JSON을 [ScoreAnswer]로 읽는다.
 * 시스템 프롬프트는 호출하는 쪽이 DB에서 읽어 넘긴다. 점수 검증과 합산은 도메인 모델이 한다.
 * Converse API에는 JSON 강제 모드가 없어 모델이 JSON을 코드 블록으로 감싸 보낼 수 있다. 그래서 첫 `{`부터 마지막 `}`까지만 읽는다.
 */
@Component
class ScoreClient(
    private val objectMapper: ObjectMapper,
    chatClientBuilder: ChatClient.Builder,
    private val scoreModelProperties: ScoreModelProperties,
) {
    private val chatClient = chatClientBuilder.build()

    fun isEnabled(): Boolean = scoreModelProperties.model.isNotBlank()

    /** [systemPrompt]를 시스템 메시지로, 번호 붙인 [submission] 이벤트를 사용자 메시지로 보낸다. */
    fun requestAnswer(submission: AnalysisSubmission, systemPrompt: String): ScoreAnswer {
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
        private const val MAX_RESPONSE_LENGTH = 100_000
        private const val SUFFICIENT_FIELD = "sufficient"
        private const val SCORES_FIELD = "scores"
        private const val EVIDENCE_FIELD = "evidence"
        private const val JSON_OBJECT_START = '{'
        private const val JSON_OBJECT_END = '}'
    }
}
