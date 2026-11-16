package com.santacut.infrastructure.analysis.client

import com.google.genai.Client
import com.santacut.domain.analysis.model.AnalysisSubmission
import com.santacut.domain.analysis.model.Archetype
import com.santacut.domain.analysis.model.ChatMessage
import com.santacut.domain.analysis.model.ChatMessages
import com.santacut.domain.analysis.model.DataSource
import com.santacut.domain.analysis.model.Score
import com.santacut.domain.analysis.model.ScoreDimension
import com.santacut.domain.analysis.model.ScoreShrinkage
import com.santacut.domain.analysis.model.ScoringChunk
import com.santacut.domain.analysis.model.ScoringChunkAnswer
import com.santacut.domain.analysis.model.ScoringChunkAnswers
import com.santacut.domain.analysis.model.ScoringChunks
import com.santacut.domain.analysis.model.ScoringDigest
import com.santacut.domain.global.model.SeoulTime
import com.santacut.domain.global.model.Sha256
import com.santacut.infrastructure.global.config.ScoreModelProperties
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.google.genai.GoogleGenAiChatModel
import org.springframework.ai.google.genai.GoogleGenAiChatOptions
import org.springframework.ai.google.genai.common.GoogleGenAiThinkingLevel
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean
import org.springframework.core.io.ClassPathResource
import tools.jackson.databind.json.JsonMapper
import java.io.File
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.assertEquals

/**
 * 채점 파이프라인(구간 나누기 → 구간별 호출 → 코드 합산 → 수축)을 실제 Gemini로 재는 수동 평가.
 * 기본 `check`에서는 빠지고 `./gradlew eval`로만 돈다. 짝 대화 확인만 API를 부르지 않는다.
 *
 * 환경 변수: GEMINI_API_KEY(필수), EVAL_REPETITIONS(짧은 대화 반복, 기본 3), EVAL_LONG_REPETITIONS(긴 대화 위치, 기본 0=건너뜀),
 * EVAL_VARIANT_REPETITIONS(길이 불변·단체 대화 감싸기, 기본 0=건너뜀), EVAL_PROMPT_FILE(채점 프롬프트를 이 파일로 바꿈).
 * 수축 세기 C는 [STRENGTHS]를 같은 답에 모두 적용해 비교한다(추가 호출 없음). 결과는 `build/eval/`에 남는다.
 * 기준(전체 맥락) 평균은 build/eval/full-baseline.txt를 다시 쓴다(5판 평가 때 잰 값).
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
    fun `짧은 대화의 반복 안정성, 기준 대비 오차, 유형 안정성을 잰다`() {
        val full = fullBaseline()
        val lines = mutableListOf("prompt=${promptLabel()} max-output-tokens=${scoreModelProperties().maxOutputTokens}")
        val runsByChat = (1..CHAT_COUNT).associateWith { chat -> List(REPETITIONS) { run(digestOf(chatMessages(chat))) } }
        STRENGTHS.forEach { strength ->
            val errors = mutableListOf<Double>()
            val deviations = mutableListOf<Double>()
            val gaps = mutableListOf<String>()
            runsByChat.forEach { (chat, runs) ->
                val scores = runs.mapNotNull { it.score(strength) }
                val totals = scores.map { it.hundredths / 100.0 }
                if (totals.size >= 2) deviations += standardDeviation(totals)
                ScoreDimension.entries.forEach { dimension ->
                    val expected = full["chat-$chat|${dimension.key}"] ?: return@forEach
                    if (scores.isNotEmpty()) errors += abs(scores.map { it.components.scoreOf(dimension).toDouble() }.average() - expected)
                }
                gaps += "chat-$chat=${totals.map { "%.1f".format(it) }} gap=%+.2f".format(totals.average() - full.getValue("chat-$chat|total"))
            }
            lines += "C=$strength mae=%.2f std-mean=%.2f std-max=%.2f ".format(errors.average(), deviations.average(), deviations.max()) + gaps
        }
        runsByChat.forEach { (chat, runs) ->
            val archetypes = runs.mapNotNull { it.score(DEFAULT_STRENGTH)?.let { score -> Archetype.of(score.components, score.playfulness, PLAYFULNESS_SPLIT) } }
            val stable = archetypes.groupingBy { it }.eachCount().values.maxOrNull() ?: 0
            lines += "chat-$chat archetypes=$archetypes stable=$stable/${runs.size} playfulness=${runs.map { it.score(DEFAULT_STRENGTH)?.playfulness }} " +
                "raw=${runs.map { it.rawLine() }} tokens(in/out)=${runs.map { "${it.inputTokens()}/${it.outputTokens()}" }} finish=${runs.map { it.finish() }}"
        }
        val all = runsByChat.values.flatten()
        lines += "per-analysis tokens in=%.0f out=%.0f cost-usd=%.5f".format(all.map { it.inputTokens() }.average(), all.map { it.outputTokens() }.average(), all.map { it.cost() }.average())
        report("stability.txt", lines.joinToString("\n"))
    }

    @Test
    fun `긴 대화에서 비꼬는 구간의 위치에 따라 점수가 달라지는지 잰다`() {
        assumeTrue(LONG_REPETITIONS > 0, "EVAL_LONG_REPETITIONS가 0이면 건너뛴다")
        val filler = (1..CHAT_COUNT).filter { it != SARCASTIC_CHAT }.flatMap { chatMessages(it) }
        val sarcastic = chatMessages(SARCASTIC_CHAT)
        val fillerCount = LONG_CHAT_EVENTS - sarcastic.size
        val variants = mapOf("start" to 0, "middle" to fillerCount / 2, "end" to fillerCount, "control" to null)
        val lines = mutableListOf("prompt=${promptLabel()}")
        val runsByVariant = variants.mapValues { (_, at) ->
            val base = List(if (at == null) LONG_CHAT_EVENTS else fillerCount) { filler[it % filler.size] }
            val messages = if (at == null) base else base.take(at) + sarcastic + base.drop(at)
            val digest = spreadSubmission(messages).toScoringDigest()
            List(if (at == null) 1 else LONG_REPETITIONS) { run(digest) }
        }
        STRENGTHS.forEach { strength ->
            val means = runsByVariant.mapValues { (_, runs) -> runs.mapNotNull { it.score(strength)?.hundredths?.div(100.0) }.average() }
            val positions = listOf("start", "middle", "end").map { means.getValue(it) }
            lines += "C=$strength means=${means.mapValues { "%.2f".format(it.value) }} position-spread=%.2f".format(positions.max() - positions.min())
        }
        runsByVariant.forEach { (name, runs) ->
            lines += "$name chunks=${runs.first().chunkCount()} chunk-positivity=${runs.map { it.chunkScores(ScoreDimension.POSITIVITY_SUPPORT) }} " +
                "tokens(in/out)=${runs.map { "${it.inputTokens()}/${it.outputTokens()}" }} cost-usd=${runs.map { "%.5f".format(it.cost()) }} finish=${runs.map { it.finish() }}"
        }
        report("long-chat-positions.txt", lines.joinToString("\n"))
    }

    @Test
    fun `같은 대화를 반으로 자르거나 두 번 이어도, 단체 대화로 감싸도 총점이 비슷한지 잰다`() {
        assumeTrue(VARIANT_REPETITIONS > 0, "EVAL_VARIANT_REPETITIONS가 0이면 건너뛴다")
        val lines = mutableListOf("prompt=${promptLabel()}")
        val deltas = STRENGTHS.associateWith { mutableMapOf<String, MutableList<Double>>() }
        (1..CHAT_COUNT).forEach { chat ->
            val messages = chatMessages(chat)
            val variants = mapOf(
                "original" to messages,
                "half" to messages.take(messages.size / 2),
                "double" to messages + messages.map { ChatMessage(it.speaker, it.occurredAt.plus(DUPLICATE_SHIFT), it.text) },
                "group" to asGroup(messages),
            )
            val runs = variants.mapValues { (_, variant) -> List(VARIANT_REPETITIONS) { run(digestOf(variant, verify = false)) } }
            STRENGTHS.forEach { strength ->
                val means = runs.mapValues { (_, list) -> list.mapNotNull { it.score(strength)?.hundredths?.div(100.0) }.average() }
                listOf("half", "double", "group").forEach { name ->
                    deltas.getValue(strength).getOrPut(name) { mutableListOf() } += abs(means.getValue(name) - means.getValue("original"))
                }
                lines += "C=$strength chat-$chat means=${means.mapValues { "%.2f".format(it.value) }}"
            }
        }
        deltas.forEach { (strength, byName) ->
            lines += "C=$strength " + byName.entries.joinToString(" ") { (name, list) -> "$name-delta mean=%.2f max=%.2f".format(list.average(), list.max()) }
        }
        report("invariance.txt", lines.joinToString("\n"))
    }

    /** 상대 줄을 두 사람(민수, 지영)이 번갈아 말한 단체 대화로 바꾼다. 본인 줄은 그대로다. */
    private fun asGroup(messages: List<ChatMessage>): List<ChatMessage> {
        var partnerTurn = 0
        return messages.map { message ->
            if (message.speaker == SELF) message else ChatMessage(GROUP_MEMBERS[partnerTurn++ % GROUP_MEMBERS.size], message.occurredAt, message.text)
        }
    }

    private fun run(digest: ScoringDigest): Run {
        val captured = mutableListOf<ScoringChunkAnswer>()
        val answers = ScoringChunks.of(digest).ask { chunk ->
            scoreClient.requestAnswer(chunk, systemPrompt).also { answer -> captured += ScoringChunkAnswer(chunk, answer) }
        }
        return Run(answers, captured)
    }

    private class Run(private val answers: ScoringChunkAnswers, private val chunks: List<ScoringChunkAnswer>) {
        fun score(strength: BigDecimal): Score? =
            runCatching { answers.toScore(ScoreShrinkage(PRIOR, strength, FULL_SELF_TURNS)) }.getOrNull()

        fun rawLine(): String = score(BigDecimal.ZERO)?.components?.toList()?.joinToString("/").orEmpty()

        fun chunkCount(): Int = chunks.size

        fun chunkScores(dimension: ScoreDimension): List<String> = chunks.map { chunkAnswer ->
            runCatching { ScoringChunkAnswers(listOf(chunkAnswer)).toScore(ScoreShrinkage.NONE).components.scoreOf(dimension).toPlainString() }
                .getOrDefault("-")
        }

        fun inputTokens(): Long = answers.usage().inputTokens

        fun outputTokens(): Long = answers.usage().billedOutputTokens()

        fun finish(): String = answers.usage().finishReason

        fun cost(): Double = inputTokens() * INPUT_USD_PER_TOKEN + outputTokens() * OUTPUT_USD_PER_TOKEN
    }

    private val scoreClient: ScoreClient by lazy {
        val properties = scoreModelProperties()
        ScoreClient(JsonMapper.builder().build(), ChatClient.builder(chatModel(properties)), properties)
    }

    private val systemPrompt: String by lazy {
        System.getenv("EVAL_PROMPT_FILE")?.let { File(it).readText() }
            ?: PROMPT_ROW.findAll(resource("prompts.sql")).maxBy { it.groupValues[1].toInt() }.groupValues[2].replace("''", "'")
    }

    private fun promptLabel(): String = System.getenv("EVAL_PROMPT_FILE")?.let { File(it).name }
        ?: "v${PROMPT_ROW.findAll(resource("prompts.sql")).maxOf { it.groupValues[1].toInt() }}"

    private fun scoreModelProperties(): ScoreModelProperties {
        val config = YamlPropertiesFactoryBean().apply { setResources(ClassPathResource("application.yml")) }.getObject()
        val maxOutputTokens = config?.getProperty("spring.ai.google.genai.chat.max-output-tokens")?.toInt() ?: DEFAULT_MAX_OUTPUT_TOKENS
        return ScoreModelProperties(ScoreModelProperties().model, maxOutputTokens)
    }

    private fun chatModel(properties: ScoreModelProperties): GoogleGenAiChatModel {
        val options = GoogleGenAiChatOptions.builder()
            .model(properties.model)
            .temperature(TEMPERATURE)
            .maxOutputTokens(properties.maxOutputTokens)
            .responseMimeType(JSON_MIME_TYPE)
            .thinkingLevel(GoogleGenAiThinkingLevel.MINIMAL)
            .build() as GoogleGenAiChatOptions
        val client = Client.builder().apiKey(System.getenv("GEMINI_API_KEY")).build()
        return GoogleGenAiChatModel.builder().genAiClient(client).options(options).build()
    }

    private fun fullBaseline(): Map<String, Double> =
        File(FULL_BASELINE_FILE).takeIf { it.exists() }?.readLines()?.filter { it.contains('=') }
            ?.associate { it.substringBefore('=') to it.substringAfter('=').toDouble() }.orEmpty()

    private fun spreadSubmission(messages: List<ChatMessage>): AnalysisSubmission {
        val now = Instant.now()
        val start = now.minus(Duration.ofDays(LONG_CHAT_DAYS))
        val step = Duration.ofDays(LONG_CHAT_DAYS).dividedBy(messages.size.toLong())
        val timed = messages.mapIndexed { index, source -> ChatMessage(source.speaker, start.plus(step.multipliedBy(index.toLong())), source.text) }
        return AnalysisSubmission(DataSource.KAKAO, ChatMessages(timed).eventsFor(SELF).recentAt(now))
    }

    private fun standardDeviation(values: List<Double>): Double {
        val mean = values.average()
        return sqrt(values.sumOf { (it - mean) * (it - mean) } / values.size)
    }

    private fun digestOf(messages: List<ChatMessage>, verify: Boolean = true): ScoringDigest {
        val digest = AnalysisSubmission(DataSource.KAKAO, ChatMessages(messages).sortedByTime().eventsFor(SELF)).toScoringDigest()
        return if (verify) digest.verifyEnough() else digest
    }

    private fun modelInputOf(messages: List<ChatMessage>): String = ScoreModelInput.from(ScoringChunk.whole(digestOf(messages, verify = false))).toText()

    private fun chatMessages(chat: Int): List<ChatMessage> = messagesOf(resource("chat-$chat.txt"))

    private fun messagesOf(text: String): List<ChatMessage> =
        text.lines().filter { it.isNotBlank() && !it.startsWith("#") }.map { line ->
            val (time, speaker, body) = line.split("|", limit = 3)
            ChatMessage(speaker, LocalDateTime.parse(time, TIME_FORMAT).atZone(SeoulTime.ZONE).toInstant(), body)
        }

    private fun blocksOf(text: String): Map<String, List<ChatMessage>> =
        text.split(BLOCK_HEADER).drop(1).associate { block -> block.substringBefore("\n").trim() to messagesOf(block.substringAfter("\n")) }

    private fun resource(name: String): String =
        ClassPathResource(if (name.endsWith(".sql")) name else "eval/$name").getContentAsString(Charsets.UTF_8)

    private fun report(name: String, text: String) {
        println(text)
        File("build/eval").apply { mkdirs() }.resolve(name).writeText(text + "\n")
    }

    companion object {
        private const val SELF = "나"
        private val GROUP_MEMBERS = listOf("민수", "지영")
        private const val PAIR_COUNT = 8
        private const val CHAT_COUNT = 5
        private const val LONG_CHAT_EVENTS = 2000
        private const val LONG_CHAT_DAYS = 89L
        private const val SARCASTIC_CHAT = 2
        private val DUPLICATE_SHIFT: Duration = Duration.ofDays(1)
        private val REPETITIONS = System.getenv("EVAL_REPETITIONS")?.toIntOrNull() ?: 3
        private val LONG_REPETITIONS = System.getenv("EVAL_LONG_REPETITIONS")?.toIntOrNull() ?: 0
        private val VARIANT_REPETITIONS = System.getenv("EVAL_VARIANT_REPETITIONS")?.toIntOrNull() ?: 0
        private val PRIOR = BigDecimal(55)
        private const val FULL_SELF_TURNS = 20
        private val STRENGTHS = listOf(BigDecimal.ZERO, BigDecimal("0.5"), BigDecimal.ONE, BigDecimal(2))
        private val DEFAULT_STRENGTH = BigDecimal("0.5")
        private const val PLAYFULNESS_SPLIT = 50
        private const val INPUT_USD_PER_TOKEN = 0.3 / 1_000_000
        private const val OUTPUT_USD_PER_TOKEN = 2.5 / 1_000_000
        private const val FULL_BASELINE_FILE = "build/eval/full-baseline.txt"
        private const val TEMPERATURE = 0.1
        private const val DEFAULT_MAX_OUTPUT_TOKENS = 250
        private const val JSON_MIME_TYPE = "application/json"
        private const val BLOCK_HEADER = "## "
        private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        private val PROMPT_ROW = Regex("'score',(\\d+),'((?:[^']++|'')*+)'\\)")
    }
}
