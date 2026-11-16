package com.santacut.infrastructure.analysis.client

import com.google.genai.Client
import com.santacut.domain.analysis.model.AnalysisSubmission
import com.santacut.domain.analysis.model.ChatMessage
import com.santacut.domain.analysis.model.ChatMessages
import com.santacut.domain.analysis.model.DataSource
import com.santacut.domain.analysis.model.ScoreDimension
import com.santacut.domain.global.model.SeoulTime
import com.santacut.domain.global.model.Sha256
import com.santacut.infrastructure.global.config.ScoreModelProperties
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.google.genai.GoogleGenAiChatModel
import org.springframework.ai.google.genai.GoogleGenAiChatOptions
import org.springframework.ai.google.genai.common.GoogleGenAiThinkingLevel
import org.springframework.ai.google.genai.metadata.GoogleGenAiUsage
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean
import org.springframework.core.io.ClassPathResource
import tools.jackson.databind.json.JsonMapper
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.assertEquals

/**
 * 채점 입력이 맥락을 잃는 정도를 재는 수동 평가. 기본 `check`에서는 빠지고 `./gradlew eval`로만 돈다.
 * 짝 대화 확인은 API를 부르지 않고, 안정성·전체 맥락 비교는 환경 변수 GEMINI_API_KEY로 실제 Gemini를 부른다.
 * 결과 표는 `build/eval/`에 남긴다.
 */
@Tag("eval")
class ScoringContextEvalTest {
    @Test
    fun `짝 대화는 상대 맥락이 달라도 모델 입력이 같다`() {
        val blocks = blocksOf(resource("pairs.txt"))
        val identical = (1..PAIR_COUNT).count { pair ->
            Sha256.hex(modelInputOf(blocks.getValue("${pair}A"))) == Sha256.hex(modelInputOf(blocks.getValue("${pair}B")))
        }
        report("pairs.txt", "pair-identical: $identical/$PAIR_COUNT")
        assertEquals(PAIR_COUNT, identical)
    }

    @Test
    fun `실제 Gemini로 반복 안정성과 전체 맥락 대비 오차를 잰다`() {
        val config = YamlPropertiesFactoryBean().apply { setResources(ClassPathResource("application.yml")) }.getObject()!!
        val maxOutputTokens = config.getProperty("spring.ai.google.genai.chat.max-output-tokens").toInt()
        val thinkingLevel = config.getProperty("spring.ai.google.genai.chat.thinking-level")?.let(GoogleGenAiThinkingLevel::valueOf)
        val client = Client.builder().apiKey(System.getenv("GEMINI_API_KEY")).build()
        val pipelineModel = chatModel(client, maxOutputTokens, thinkingLevel)
        val fullModel = chatModel(client, FULL_CONTEXT_MAX_OUTPUT_TOKENS, thinkingLevel)
        val systemPrompt = latestScorePrompt()
        val lines = mutableListOf("max-output-tokens=$maxOutputTokens thinking-level=$thinkingLevel")
        val calls = mutableListOf<Call>()
        val errors = mutableListOf<Double>()
        val deviations = mutableListOf<Double>()
        (1..CHAT_COUNT).forEach { chat ->
            val messages = messagesOf(resource("chat-$chat.txt"))
            val input = ScoreModelInput.from(submissionOf(messages).toScoringDigest().verifySampled()).toText()
            val pipeline = List(REPETITIONS) { call(pipelineModel, systemPrompt, input) }
            val full = List(REPETITIONS) { call(fullModel, FULL_CONTEXT_PROMPT, transcriptOf(messages)) }
            calls += pipeline
            val totals = pipeline.mapNotNull { it.total() }
            if (totals.size == REPETITIONS) deviations += standardDeviation(totals)
            val dimensionErrors = ScoreDimension.entries.mapNotNull { dimension ->
                val scored = meanOf(pipeline, dimension) ?: return@mapNotNull null
                abs(scored - (meanOf(full, dimension) ?: return@mapNotNull null)).also { errors += it }
            }
            lines += "chat-$chat totals=$totals full=${full.mapNotNull { it.total() }} " +
                "mae=${dimensionErrors.map { "%.1f".format(it) }} finish=${pipeline.map { it.finishReason }} " +
                "tokens(in/out/thought/total)=${pipeline.map { "${it.inputTokens}/${it.outputTokens}/${it.thoughtTokens}/${it.totalTokens}" }}"
        }
        lines += "total-std-dev mean=%.2f max=%.2f".format(deviations.average(), deviations.max())
        lines += "mae-vs-full mean=%.2f over ${errors.size / ScoreDimension.entries.size} scored chats".format(errors.average())
        lines += "thought-tokens mean=%.1f max=%d".format(calls.map { it.thoughtTokens }.average(), calls.maxOf { it.thoughtTokens })
        lines += "max-tokens-finishes=${calls.count { it.finishReason == "MAX_TOKENS" }}/${calls.size} no-scores(insufficient/unreadable)=${calls.count { it.scores.isEmpty() }}"
        report("stability.txt", lines.joinToString("\n"))
    }

    private fun chatModel(client: Client, maxOutputTokens: Int, thinkingLevel: GoogleGenAiThinkingLevel?): GoogleGenAiChatModel {
        val options = GoogleGenAiChatOptions.builder()
            .model(ScoreModelProperties().model)
            .temperature(TEMPERATURE)
            .maxOutputTokens(maxOutputTokens)
            .responseMimeType(JSON_MIME_TYPE)
            .apply { thinkingLevel?.let { thinkingLevel(it) } }
            .build() as GoogleGenAiChatOptions
        return GoogleGenAiChatModel.builder().genAiClient(client).options(options).build()
    }

    private fun call(model: GoogleGenAiChatModel, systemPrompt: String, userText: String): Call {
        val response = model.call(Prompt(listOf(SystemMessage(systemPrompt), UserMessage(userText))))
        val usage = response.metadata.usage
        val text = response.result?.output?.text.orEmpty()
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        val scores = if (start in 0..<end) {
            runCatching { JSON.readTree(text.substring(start, end + 1)).path("scores") }.getOrNull()
                ?.let { node -> ScoreDimension.entries.filter { node.has(it.key) }.associateWith { node.path(it.key).asDouble() } }
                .orEmpty()
        } else {
            emptyMap()
        }
        return Call(
            scores,
            usage.promptTokens.toLong(),
            usage.completionTokens.toLong(),
            ((usage as? GoogleGenAiUsage)?.thoughtsTokenCount ?: 0).toLong(),
            response.result?.metadata?.finishReason.orEmpty(),
            usage.totalTokens.toLong(),
        )
    }

    private fun meanOf(calls: List<Call>, dimension: ScoreDimension): Double? =
        calls.mapNotNull { it.scores[dimension] }.takeIf { it.isNotEmpty() }?.average()

    private fun standardDeviation(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        return sqrt(values.sumOf { (it - mean) * (it - mean) } / values.size)
    }

    private fun modelInputOf(messages: List<ChatMessage>): String = ScoreModelInput.from(submissionOf(messages).toScoringDigest()).toText()

    private fun submissionOf(messages: List<ChatMessage>): AnalysisSubmission =
        AnalysisSubmission(DataSource.KAKAO, ChatMessages(messages).sortedByTime().eventsWrittenBy(SELF))

    private fun transcriptOf(messages: List<ChatMessage>): String =
        messages.joinToString("\n") { "${TIME_FORMAT.format(it.occurredAt.atZone(SeoulTime.ZONE))} ${it.speaker}: ${it.text}" }

    private fun messagesOf(text: String): List<ChatMessage> =
        text.lines().filter { it.isNotBlank() && !it.startsWith("#") }.map { line ->
            val (time, speaker, body) = line.split("|", limit = 3)
            ChatMessage(speaker, LocalDateTime.parse(time, TIME_FORMAT).atZone(SeoulTime.ZONE).toInstant(), body)
        }

    private fun blocksOf(text: String): Map<String, List<ChatMessage>> =
        text.split(BLOCK_HEADER).drop(1).associate { block ->
            block.substringBefore("\n").trim() to messagesOf(block.substringAfter("\n"))
        }

    private fun latestScorePrompt(): String =
        PROMPT_ROW.findAll(resource("prompts.sql")).maxBy { it.groupValues[1].toInt() }.groupValues[2].replace("''", "'")

    private fun resource(name: String): String =
        ClassPathResource(if (name.endsWith(".sql")) name else "eval/$name").getContentAsString(Charsets.UTF_8)

    private fun report(name: String, text: String) {
        println(text)
        File("build/eval").apply { mkdirs() }.resolve(name).writeText(text + "\n")
    }

    private data class Call(
        val scores: Map<ScoreDimension, Double>,
        val inputTokens: Long,
        val outputTokens: Long,
        val thoughtTokens: Long,
        val finishReason: String,
        val totalTokens: Long,
    ) {
        fun total(): Double? =
            if (scores.size == ScoreDimension.entries.size) scores.entries.sumOf { (dimension, score) -> score * dimension.weight } / 100 else null
    }

    companion object {
        private const val SELF = "나"
        private const val PAIR_COUNT = 8
        private const val CHAT_COUNT = 5
        private const val REPETITIONS = 3
        private const val TEMPERATURE = 0.1
        private const val JSON_MIME_TYPE = "application/json"
        private const val FULL_CONTEXT_MAX_OUTPUT_TOKENS = 1024
        private const val BLOCK_HEADER = "## "
        private val JSON = JsonMapper.builder().build()
        private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        private val PROMPT_ROW = Regex("'score',(\\d+),'((?:[^']|'')*)'\\)")
        private val FULL_CONTEXT_PROMPT = """
            너는 재미용 산타 점수를 매기는 채점자다. 입력은 '나'와 '상대'의 대화 전체이며 각 줄 앞에 시각이 있다.
            '나'의 행동만 평가하고, 상대 메시지와 답장 간격은 '나'의 말을 이해하는 맥락으로 쓴다. 비꼬는 말은 근거가 아니다.
            - care_empathy: 상대의 상태를 살피고 공감하는 행동
            - positivity_support: 구체적인 격려, 응원, 감사
            - respectful_communication: 존중하는 말투, 사과, 부탁. 욕설이나 비하가 섞이면 낮춘다.
            - cooperation_sharing: 약속을 잡고 지키기, 돕기, 나누기 같은 구체적인 협력
            항목마다 0~100 정수다. 평범한 대화는 50~65, 분명하고 반복되는 좋은 행동은 75 이상, 아주 두드러지면 90 이상, 반대 행동이 뚜렷하면 50 미만이다.
            마크다운 없이 JSON 하나만 돌려준다: {"scores":{"care_empathy":0,"positivity_support":0,"respectful_communication":0,"cooperation_sharing":0}}
        """.trimIndent()
    }
}
