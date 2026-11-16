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
import org.junit.jupiter.api.Assumptions.assumeTrue
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
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.assertEquals

/**
 * 채점 입력이 맥락을 잃는 정도를 재는 수동 평가. 기본 `check`에서는 빠지고 `./gradlew eval`로만 돈다.
 * 짝 대화 확인은 API를 부르지 않고, 안정성·전체 맥락 비교와 긴 대화 토큰 세기는 환경 변수 GEMINI_API_KEY로 실제 Gemini를 부른다.
 * 반복 횟수는 환경 변수 EVAL_REPETITIONS(기본 3)로 바꾼다. 긴 대화 위치 평가는 EVAL_LONG_REPETITIONS(기본 0, 건너뜀)로 켠다.
 * 기준(전체 맥락) 평균은 build/eval/full-baseline.txt에 두고 다음 실행에서 다시 쓴다.
 * 결과 표는 `build/eval/`에 남긴다.
 */
@Tag("eval")
class ScoringContextEvalTest {
    @Test
    fun `짝 대화는 상대 맥락이 다르면 모델 입력도 다르다`() {
        val blocks = blocksOf(resource("pairs.txt"))
        val distinguished = (1..PAIR_COUNT).count { pair ->
            Sha256.hex(modelInputOf(blocks.getValue("${pair}A"))) != Sha256.hex(modelInputOf(blocks.getValue("${pair}B")))
        }
        report("pairs.txt", "pair-distinguished: $distinguished/$PAIR_COUNT")
        assertEquals(PAIR_COUNT, distinguished)
    }

    @Test
    fun `이벤트 2,000개짜리 긴 대화의 입력 토큰을 센다`() {
        val client = Client.builder().apiKey(System.getenv("GEMINI_API_KEY")).build()
        val model = ScoreModelProperties().model
        val corpus = (1..CHAT_COUNT).flatMap { messagesOf(resource("chat-$it.txt")) }
        val now = Instant.now()
        val start = now.minus(Duration.ofDays(LONG_CHAT_DAYS))
        val step = Duration.ofDays(LONG_CHAT_DAYS).dividedBy(LONG_CHAT_EVENTS.toLong())
        val messages = List(LONG_CHAT_EVENTS) { index ->
            val source = corpus[index % corpus.size]
            ChatMessage(source.speaker, start.plus(step.multipliedBy(index.toLong())), source.text)
        }
        val events = ChatMessages(messages).eventsFor(SELF).recentAt(now)
        val digest = AnalysisSubmission(DataSource.KAKAO, events).toScoringDigest()
        val input = ScoreModelInput.from(digest)
        val systemPrompt = latestScorePrompt()
        val counted = countTokens(client, model, systemPrompt) + countTokens(client, model, input.toText())
        val estimated = ScoreClient.estimatedTokens(systemPrompt.length + input.toText().length)
        report(
            "long-chat.txt",
            "events=${events.size()} lines=${digest.lineCount()} omitted=${digest.omittedTurns} chars=${systemPrompt.length + input.toText().length} " +
                "counted-input-tokens=$counted estimated=$estimated",
        )
    }

    private fun countTokens(client: Client, model: String, text: String): Int =
        client.models.countTokens(model, text, null).totalTokens().orElse(0)

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
        val cachedFull = fullBaselineCache()
        val fullMeans = mutableMapOf<String, Double>()
        (1..CHAT_COUNT).forEach { chat ->
            val messages = messagesOf(resource("chat-$chat.txt"))
            val input = ScoreModelInput.from(submissionOf(messages).toScoringDigest().verifyEnough()).toText()
            val pipeline = List(REPETITIONS) { call(pipelineModel, systemPrompt, input) }
            val full = cachedFull.filterKeys { it.startsWith("chat-$chat|") }.ifEmpty {
                fullMeansOf(List(REPETITIONS) { call(fullModel, FULL_CONTEXT_PROMPT, transcriptOf(messages)) })
                    .mapKeys { (key, _) -> "chat-$chat|$key" }
            }
            fullMeans += full
            calls += pipeline
            val totals = pipeline.mapNotNull { it.total() }
            if (totals.size == REPETITIONS) deviations += standardDeviation(totals)
            val dimensionErrors = ScoreDimension.entries.mapNotNull { dimension ->
                val scored = meanOf(pipeline, dimension) ?: return@mapNotNull null
                abs(scored - (full["chat-$chat|${dimension.key}"] ?: return@mapNotNull null)).also { errors += it }
            }
            val fullTotal = full.getValue("chat-$chat|total")
            lines += "chat-$chat totals=$totals full-mean=%.2f gap=%+.2f ".format(fullTotal, totals.average() - fullTotal) +
                "mae=${dimensionErrors.map { "%.1f".format(it) }} finish=${pipeline.map { it.finishReason }} " +
                "tone-ordered=${pipeline.count { it.toneOrdered }}/${pipeline.size} " +
                "tokens(in/out/thought/total)=${pipeline.map { "${it.inputTokens}/${it.outputTokens}/${it.thoughtTokens}/${it.totalTokens}" }}"
        }
        if (cachedFull.isEmpty()) {
            File(FULL_BASELINE_FILE).apply { parentFile.mkdirs() }.writeText(fullMeans.entries.joinToString("\n") { "${it.key}=${it.value}" })
        }
        lines += "total-std-dev mean=%.2f max=%.2f".format(deviations.average(), deviations.max())
        lines += "mae-vs-full mean=%.2f over ${errors.size / ScoreDimension.entries.size} scored chats".format(errors.average())
        lines += "thought-tokens mean=%.1f max=%d".format(calls.map { it.thoughtTokens }.average(), calls.maxOf { it.thoughtTokens })
        lines += "max-tokens-finishes=${calls.count { it.finishReason == "MAX_TOKENS" }}/${calls.size} no-scores(insufficient/unreadable)=${calls.count { it.scores.isEmpty() }}"
        lines += "insufficient-evidence=${calls.count { it.insufficient }}/${calls.size} weak-dimensions=${calls.sumOf { it.weakDimensions }}"
        lines += "input-tokens mean=%.0f max=%d".format(calls.map { it.inputTokens }.average(), calls.maxOf { it.inputTokens })
        report("stability.txt", lines.joinToString("\n"))
    }

    /** 기준(전체 맥락) 프롬프트는 단계마다 같으므로 처음 잰 평균을 [FULL_BASELINE_FILE]에 두고 다시 쓴다. 지우면 새로 잰다. */
    private fun fullBaselineCache(): Map<String, Double> =
        File(FULL_BASELINE_FILE).takeIf { it.exists() }?.readLines()?.filter { it.contains('=') }
            ?.associate { it.substringBefore('=') to it.substringAfter('=').toDouble() }.orEmpty()

    private fun fullMeansOf(full: List<Call>): Map<String, Double> =
        ScoreDimension.entries.mapNotNull { dimension -> meanOf(full, dimension)?.let { dimension.key to it } }.toMap() +
            ("total" to full.mapNotNull { it.total() }.average())

    @Test
    fun `긴 대화에서 비꼬는 구간의 위치에 따라 점수가 달라지는지 잰다`() {
        assumeTrue(LONG_REPETITIONS > 0, "EVAL_LONG_REPETITIONS가 0이면 건너뛴다")
        val config = YamlPropertiesFactoryBean().apply { setResources(ClassPathResource("application.yml")) }.getObject()!!
        val maxOutputTokens = config.getProperty("spring.ai.google.genai.chat.max-output-tokens").toInt()
        val thinkingLevel = config.getProperty("spring.ai.google.genai.chat.thinking-level")?.let(GoogleGenAiThinkingLevel::valueOf)
        val model = chatModel(Client.builder().apiKey(System.getenv("GEMINI_API_KEY")).build(), maxOutputTokens, thinkingLevel)
        val systemPrompt = latestScorePrompt()
        val filler = (1..CHAT_COUNT).filter { it != SARCASTIC_CHAT }.flatMap { messagesOf(resource("chat-$it.txt")) }
        val sarcastic = messagesOf(resource("chat-$SARCASTIC_CHAT.txt"))
        val fillerCount = LONG_CHAT_EVENTS - sarcastic.size
        val variants = mapOf("start" to 0, "middle" to fillerCount / 2, "end" to fillerCount, "control" to null)
            .filter { (name, _) -> name != "control" || LONG_CONTROL }
        val lines = mutableListOf<String>()
        val means = mutableMapOf<String, Double>()
        variants.forEach { (name, at) ->
            val base = List(if (at == null) LONG_CHAT_EVENTS else fillerCount) { filler[it % filler.size] }
            val messages = if (at == null) base else base.take(at) + sarcastic + base.drop(at)
            val digest = longSubmissionOf(messages).toScoringDigest()
            val input = ScoreModelInput.from(digest).toText()
            val repetitions = if (at == null) maxOf(1, LONG_REPETITIONS / 2) else LONG_REPETITIONS
            val calls = List(repetitions) { call(model, systemPrompt, input) }
            val totals = calls.mapNotNull { it.total() }
            means[name] = totals.average()
            lines += "$name lines=${digest.lineCount()} omitted=${digest.omittedTurns} totals=$totals mean=%.2f std=%.2f ".format(totals.average(), standardDeviation(totals)) +
                "in-tokens=${calls.map { it.inputTokens }} out=${calls.map { it.outputTokens }} finish=${calls.map { it.finishReason }} " +
                "tone-ordered=${calls.count { it.toneOrdered }}/${calls.size}"
        }
        val positions = listOf("start", "middle", "end").map { means.getValue(it) }
        lines += "position-gap(max-min)=%.2f middle-vs-ends=%+.2f".format(positions.max() - positions.min(), positions[1] - (positions[0] + positions[2]) / 2)
        report("long-chat-positions.txt", lines.joinToString("\n"))
    }

    private fun longSubmissionOf(messages: List<ChatMessage>): AnalysisSubmission {
        val now = Instant.now()
        val start = now.minus(Duration.ofDays(LONG_CHAT_DAYS))
        val step = Duration.ofDays(LONG_CHAT_DAYS).dividedBy(messages.size.toLong())
        val timed = messages.mapIndexed { index, source -> ChatMessage(source.speaker, start.plus(step.multipliedBy(index.toLong())), source.text) }
        return AnalysisSubmission(DataSource.KAKAO, ChatMessages(timed).eventsFor(SELF).recentAt(now))
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
        val tree = if (start in 0..<end) runCatching { JSON.readTree(text.substring(start, end + 1)) }.getOrNull() else null
        val scores = tree?.path("scores")
            ?.let { node -> ScoreDimension.entries.filter { node.has(it.key) }.associateWith { node.path(it.key).asDouble() } }
            .orEmpty()
        val evidence = tree?.path("evidence")
        val toneAt = text.indexOf("\"tone\"")
        val toneOrdered = toneAt > text.indexOf("\"evidence\"") && toneAt < text.indexOf("\"scores\"")
        val weak = ScoreDimension.entries.count { evidence?.path(it.key)?.let { node -> node.isArray && node.isEmpty } == true }
        return Call(
            scores,
            tree?.path("sufficient")?.asBoolean(true) == false,
            weak,
            usage.promptTokens.toLong(),
            usage.completionTokens.toLong(),
            ((usage as? GoogleGenAiUsage)?.thoughtsTokenCount ?: 0).toLong(),
            response.result?.metadata?.finishReason.orEmpty(),
            usage.totalTokens.toLong(),
            toneOrdered,
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
        AnalysisSubmission(DataSource.KAKAO, ChatMessages(messages).sortedByTime().eventsFor(SELF))

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
        val insufficient: Boolean,
        val weakDimensions: Int,
        val inputTokens: Long,
        val outputTokens: Long,
        val thoughtTokens: Long,
        val finishReason: String,
        val totalTokens: Long,
        val toneOrdered: Boolean,
    ) {
        fun total(): Double? =
            if (scores.size == ScoreDimension.entries.size) scores.entries.sumOf { (dimension, score) -> score * dimension.weight } / 100 else null
    }

    companion object {
        private const val SELF = "나"
        private const val PAIR_COUNT = 8
        private const val CHAT_COUNT = 5
        private const val LONG_CHAT_EVENTS = 2000
        private const val LONG_CHAT_DAYS = 89L
        private val REPETITIONS = System.getenv("EVAL_REPETITIONS")?.toIntOrNull() ?: 3
        private val LONG_REPETITIONS = System.getenv("EVAL_LONG_REPETITIONS")?.toIntOrNull() ?: 0
        private val LONG_CONTROL = System.getenv("EVAL_LONG_CONTROL") != "0"
        private const val SARCASTIC_CHAT = 2
        private const val FULL_BASELINE_FILE = "build/eval/full-baseline.txt"
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
