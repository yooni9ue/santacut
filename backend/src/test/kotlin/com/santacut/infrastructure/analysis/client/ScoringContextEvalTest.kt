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
import com.santacut.domain.global.config.ScoreShrinkageProperties
import com.santacut.domain.global.model.SeoulTime
import com.santacut.domain.global.model.Sha256
import com.santacut.infrastructure.global.config.ScoreModelProperties
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.prompt.Prompt
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
import java.util.Collections
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.assertEquals

/**
 * 채점 파이프라인(구간 나누기 → 구간별 호출 → 코드 합산 → 수축)을 실제 Gemini로 재는 수동 평가.
 * 기본 `check`에서는 빠지고 `./gradlew eval`로만 돈다. 짝 대화 확인만 API를 부르지 않는다.
 *
 * 환경 변수: GEMINI_API_KEY(필수), EVAL_REPETITIONS(짧은 대화 반복, 기본 3), EVAL_SAMPLES(한 구간 대화의 중앙값 호출 수, 기본 1), EVAL_LONG_REPETITIONS(긴 대화 위치, 기본 0=건너뜀),
 * EVAL_VARIANT_REPETITIONS(길이 불변·단체 대화 감싸기, 기본 0=건너뜀), EVAL_PROMPT_FILE(채점 프롬프트를 이 파일로 바꿈),
 * EVAL_GOLDEN_DIR(사람이 매긴 골든셋 라벨 파일 폴더, 없으면 건너뜀. 형식은 backend-design.md §4.6).
 * 수축 규칙은 [SHRINKAGES](세기 C, 근거 무게를 깎지 않는 본인 턴 수 F)를 같은 답에 모두 적용해 비교한다(추가 호출 없음).
 * 길이 불변은 기준 프롬프트로 원래·반 대화도 채점해, 모델의 길이별 변화가 기준의 변화와 얼마나 다른지도 잰다.
 * 결과는 `build/eval/`에 남는다.
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
        val lines = mutableListOf("prompt=${promptLabel()} samples=$SAMPLES max-output-tokens=${scoreModelProperties().maxOutputTokens}")
        val runsByChat = (1..CHAT_COUNT).associateWith { chat -> List(REPETITIONS) { run(digestOf(chatMessages(chat))) } }
        SHRINKAGES.forEach { shrinkage ->
            val errors = mutableListOf<Double>()
            val deviations = mutableListOf<Double>()
            val gaps = mutableListOf<String>()
            runsByChat.forEach { (chat, runs) ->
                val scores = runs.mapNotNull { it.score(shrinkage) }
                val totals = scores.map { it.hundredths / 100.0 }
                if (totals.size >= 2) deviations += standardDeviation(totals)
                ScoreDimension.entries.forEach { dimension ->
                    val expected = full["chat-$chat|${dimension.key}"] ?: return@forEach
                    if (scores.isNotEmpty()) errors += abs(scores.map { it.components.scoreOf(dimension).toDouble() }.average() - expected)
                }
                gaps += "chat-$chat=${totals.map { "%.1f".format(it) }} gap=%+.2f".format(totals.average() - full.getValue("chat-$chat|total"))
            }
            lines += "${label(shrinkage)} mae=%.2f std-mean=%.2f std-max=%.2f ".format(errors.average(), deviations.average(), deviations.max()) + gaps
        }
        val flips = mutableListOf<FlipCount>()
        runsByChat.forEach { (chat, runs) ->
            val scores = runs.mapNotNull { it.score(DEFAULT_SHRINKAGE) }
            val archetypes = scores.map { score -> Archetype.of(score.components, score.playfulness, PLAYFULNESS_SPLIT, score.rawMeans) }
            val legacy = scores.map { score -> legacyArchetype(score) }
            flips += FlipCount(legacy.distinct().size > 1, archetypes.distinct().size > 1, disagreeingPairs(legacy), disagreeingPairs(archetypes), pairs(legacy.size))
            val stable = archetypes.groupingBy { it }.eachCount().values.maxOrNull() ?: 0
            lines += "chat-$chat archetypes=$archetypes legacy=$legacy stable=$stable/${runs.size} playfulness=${runs.map { it.score(DEFAULT_SHRINKAGE)?.playfulness }} " +
                "shrunk=${runs.map { it.score(DEFAULT_SHRINKAGE)?.components?.toList()?.joinToString("/") }} raw=${runs.map { it.rawLine() }} tokens(in/cached/out)=${runs.map { "${it.inputTokens()}/${it.cachedTokens()}/${it.outputTokens()}" }} finish=${runs.map { it.finish() }}"
        }
        lines += "archetype-flip before(chats)=${flips.count { it.legacyFlipped }}/${flips.size} after(chats)=${flips.count { it.flipped }}/${flips.size} " +
            "before(pairs)=${flips.sumOf { it.legacyPairs }}/${flips.sumOf { it.pairs }} after(pairs)=${flips.sumOf { it.disagreeingPairs }}/${flips.sumOf { it.pairs }}"
        val all = runsByChat.values.flatten()
        lines += "per-analysis tokens in=%.0f cached=%.0f out=%.0f cost-usd=%.5f".format(all.map { it.inputTokens() }.average(), all.map { it.cachedTokens() }.average(), all.map { it.outputTokens() }.average(), all.map { it.cost() }.average())
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
        SHRINKAGES.forEach { shrinkage ->
            val means = runsByVariant.mapValues { (_, runs) -> runs.mapNotNull { it.score(shrinkage)?.hundredths?.div(100.0) }.average() }
            val positions = listOf("start", "middle", "end").map { means.getValue(it) }
            lines += "${label(shrinkage)} means=${means.mapValues { "%.2f".format(it.value) }} position-spread=%.2f".format(positions.max() - positions.min())
        }
        runsByVariant.forEach { (name, runs) ->
            lines += "$name chunks=${runs.first().chunkCount()} chunk-positivity=${runs.map { it.chunkScores(ScoreDimension.POSITIVITY_SUPPORT) }} " +
                "tokens(in/cached/out)=${runs.map { "${it.inputTokens()}/${it.cachedTokens()}/${it.outputTokens()}" }} cost-usd=${runs.map { "%.5f".format(it.cost()) }} finish=${runs.map { it.finish() }}"
        }
        report("long-chat-positions.txt", lines.joinToString("\n"))
    }

    @Test
    fun `같은 대화를 반으로 자르거나 두 번 이어도, 단체 대화로 감싸도 총점이 비슷한지 잰다`() {
        assumeTrue(VARIANT_REPETITIONS > 0, "EVAL_VARIANT_REPETITIONS가 0이면 건너뛴다")
        val lines = mutableListOf("prompt=${promptLabel()}")
        val deltas = SHRINKAGES.associateWith { mutableMapOf<String, MutableList<Double>>() }
        val baseline = cachedScores(LENGTH_BASELINE_FILE)
        (1..CHAT_COUNT).forEach { chat ->
            val messages = chatMessages(chat)
            val variants = mapOf(
                "original" to messages,
                "half" to messages.take(messages.size / 2),
                "double" to messages + messages.map { ChatMessage(it.speaker, it.occurredAt.plus(DUPLICATE_SHIFT), it.text) },
                "group" to asGroup(messages),
            ).filterKeys { name -> name == "original" || name in VARIANT_NAMES }
            val runs = variants.mapValues { (_, variant) -> List(VARIANT_REPETITIONS) { run(digestOf(variant, verify = false)) } }
            val baseOriginal = baseline.getOrPut("chat-$chat|original") { baselineTotal(messages) }
            val baseHalf = baseline.getOrPut("chat-$chat|half") { baselineTotal(variants.getValue("half")) }
            lines += "baseline chat-$chat original=%.2f half=%.2f".format(baseOriginal, baseHalf)
            SHRINKAGES.forEach { shrinkage ->
                val means = runs.mapValues { (_, list) -> list.mapNotNull { it.score(shrinkage)?.hundredths?.div(100.0) }.average() }
                variants.keys.filter { name -> name != "original" }.forEach { name ->
                    deltas.getValue(shrinkage).getOrPut(name) { mutableListOf() } += abs(means.getValue(name) - means.getValue("original"))
                }
                val modelChange = means.getValue("half") - means.getValue("original")
                deltas.getValue(shrinkage).getOrPut("half-vs-baseline") { mutableListOf() } += abs(modelChange - (baseHalf - baseOriginal))
                lines += "${label(shrinkage)} chat-$chat means=${means.mapValues { "%.2f".format(it.value) }}"
            }
            runs.forEach { (name, list) -> lines += "raw chat-$chat $name ${list.map { it.answerLine() }}" }
        }
        writeScores(LENGTH_BASELINE_FILE, baseline)
        deltas.forEach { (shrinkage, byName) ->
            lines += "${label(shrinkage)} " + byName.entries.joinToString(" ") { (name, list) -> "$name-delta mean=%.2f max=%.2f".format(list.average(), list.max()) }
        }
        report("invariance.txt", lines.joinToString("\n"))
    }

    @Test
    fun `사람이 매긴 골든셋과 순위 상관, 항목 오차를 잰다`() {
        val files = GOLDEN_DIR?.let { File(it).listFiles { file -> file.extension == "txt" }?.sortedBy { it.name } }.orEmpty()
        assumeTrue(files.isNotEmpty(), "EVAL_GOLDEN_DIR에 라벨 파일이 없으면 건너뛴다")
        val rows = files.mapNotNull { file -> goldenRow(file) }
        val errors = rows.flatMap { row -> ScoreDimension.entries.map { abs(row.model.components.scoreOf(it).toDouble() - row.labels.getValue(it.key)) } }
        val lines = rows.map { row -> "${row.name} human=%.2f model=%.2f".format(row.humanTotal(), row.model.hundredths / 100.0) }
        report(
            "golden.txt",
            (lines + "chats=${rows.size}/${files.size} spearman=%.3f (target>=%.1f) mae=%.2f".format(
                spearman(rows.map { it.humanTotal() }, rows.map { it.model.hundredths / 100.0 }), GOLDEN_SPEARMAN_TARGET, errors.average(),
            )).joinToString("\n"),
        )
    }

    /** 라벨 파일 하나를 채점한다. `#labels key=점수 ...` 줄이 없거나 채점에 실패하면 null이다. */
    private fun goldenRow(file: File): GoldenRow? {
        val text = file.readText()
        val labels = text.lines().firstOrNull { it.startsWith(GOLDEN_LABEL_PREFIX) }?.removePrefix(GOLDEN_LABEL_PREFIX)?.trim()?.split(" ")
            ?.associate { it.substringBefore("=") to it.substringAfter("=").toDouble() } ?: return null
        val score = run(digestOf(messagesOf(text), verify = false)).score(DEFAULT_SHRINKAGE) ?: return null
        return GoldenRow(file.nameWithoutExtension, labels, score)
    }

    private class GoldenRow(val name: String, val labels: Map<String, Double>, val model: Score) {
        fun humanTotal(): Double = ScoreDimension.entries.sumOf { it.weight * labels.getValue(it.key) } / ScoreDimension.TOTAL_WEIGHT
    }

    /** 스피어만 순위 상관. 동점은 평균 순위를 준다. */
    private fun spearman(left: List<Double>, right: List<Double>): Double {
        val leftRanks = ranks(left)
        val rightRanks = ranks(right)
        val leftMean = leftRanks.average()
        val rightMean = rightRanks.average()
        val covariance = leftRanks.indices.sumOf { (leftRanks[it] - leftMean) * (rightRanks[it] - rightMean) }
        return covariance / sqrt(leftRanks.sumOf { (it - leftMean) * (it - leftMean) } * rightRanks.sumOf { (it - rightMean) * (it - rightMean) })
    }

    private fun ranks(values: List<Double>): List<Double> =
        values.map { value -> values.count { it < value } + (values.count { it == value } + 1) / 2.0 }

    /** 상대 줄을 두 사람(민수, 지영)이 번갈아 말한 단체 대화로 바꾼다. 본인 줄은 그대로다. */
    private fun asGroup(messages: List<ChatMessage>): List<ChatMessage> {
        var partnerTurn = 0
        return messages.map { message ->
            if (message.speaker == SELF) message else ChatMessage(GROUP_MEMBERS[partnerTurn++ % GROUP_MEMBERS.size], message.occurredAt, message.text)
        }
    }

    /**
     * 띠를 두기 전 규칙: 저장한 항목 점수의 위 두 항목 차이가 5점보다 작으면 균형형이다.
     * 수축 전 평균을 넘기지 않으면 띠 안에서도 저장한 점수의 5점 기준으로 가르므로 예전 규칙과 같다.
     */
    private fun legacyArchetype(score: Score): Archetype =
        Archetype.of(score.components, score.playfulness, PLAYFULNESS_SPLIT)

    private fun disagreeingPairs(archetypes: List<Archetype>): Int =
        archetypes.indices.sumOf { left -> (left + 1 until archetypes.size).count { right -> archetypes[left] != archetypes[right] } }

    private fun pairs(size: Int): Int = size * (size - 1) / 2

    private class FlipCount(val legacyFlipped: Boolean, val flipped: Boolean, val legacyPairs: Int, val disagreeingPairs: Int, val pairs: Int)

    private fun run(digest: ScoringDigest): Run {
        val captured = Collections.synchronizedList(mutableListOf<ScoringChunkAnswer>())
        val answers = ScoringChunks.of(digest).ask(SAMPLES, ScoreAnswerCalls::concurrently, { chunks, ask -> ScoreAnswerCalls.each(chunks, chunks.size, ask) }) { chunk ->
            scoreClient.requestAnswer(chunk, systemPrompt).also { answer -> captured += ScoringChunkAnswer(chunk, answer) }
        }
        return Run(answers, captured)
    }

    private class Run(private val answers: ScoringChunkAnswers, private val chunks: List<ScoringChunkAnswer>) {
        fun score(shrinkage: ScoreShrinkage): Score? = runCatching { answers.toScore(shrinkage) }.getOrNull()

        fun rawLine(): String = score(ScoreShrinkage(BigDecimal.ZERO, 1, personalPrior = false))?.components?.toList()?.joinToString("/").orEmpty()

        fun chunkCount(): Int = chunks.size

        fun answerLine(): String = chunks.joinToString(" | ") { chunkAnswer ->
            "turns=${chunkAnswer.chunk.selfTurnCount()} " + ScoreDimension.entries.indices.joinToString(" ") { position ->
                "${chunkAnswer.answer.scoreAt(position)}/n${chunkAnswer.answer.opportunityAt(position)}/e${chunkAnswer.answer.evidenceAt(position).size}"
            }
        }

        fun chunkScores(dimension: ScoreDimension): List<String> = chunks.map { chunkAnswer ->
            runCatching { ScoringChunkAnswers(listOf(chunkAnswer)).toScore(ScoreShrinkage(BigDecimal.ZERO, 1, personalPrior = false)).components.scoreOf(dimension).toPlainString() }
                .getOrDefault("-")
        }

        fun inputTokens(): Long = answers.usage().inputTokens

        fun outputTokens(): Long = answers.usage().billedOutputTokens()

        fun finish(): String = answers.usage().finishReason

        fun cachedTokens(): Long = answers.usage().cachedInputTokens

        fun cost(): Double =
            (inputTokens() - cachedTokens()) * INPUT_USD_PER_TOKEN + cachedTokens() * CACHED_INPUT_USD_PER_TOKEN + outputTokens() * OUTPUT_USD_PER_TOKEN
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

    private fun fullBaseline(): Map<String, Double> = cachedScores(FULL_BASELINE_FILE)

    private fun cachedScores(path: String): MutableMap<String, Double> =
        File(path).takeIf { it.exists() }?.readLines()?.filter { it.contains('=') }
            ?.associate { it.substringBefore('=') to it.substringAfter('=').toDouble() }.orEmpty().toMutableMap()

    private fun writeScores(path: String, scores: Map<String, Double>) {
        File(path).apply { parentFile.mkdirs() }.writeText(scores.entries.joinToString("\n") { "${it.key}=${it.value}" } + "\n")
    }

    private fun label(shrinkage: ScoreShrinkage): String = "C=${shrinkage.strength} F=${shrinkage.fullSelfTurns}" + if (shrinkage.personalPrior) " P" else ""

    /** 기준(전체 맥락) 프롬프트로 [messages]를 [BASELINE_REPETITIONS]번 채점한 가중 총점 평균. 5판 평가 때와 같은 프롬프트다. */
    private fun baselineTotal(messages: List<ChatMessage>): Double =
        List(BASELINE_REPETITIONS) { baselineCall(messages) }.filterNotNull().average()

    private fun baselineCall(messages: List<ChatMessage>): Double? {
        val transcript = messages.joinToString("\n") { "${TIME_FORMAT.format(it.occurredAt.atZone(SeoulTime.ZONE))} ${it.speaker}: ${it.text}" }
        val response = baselineModel.call(Prompt(listOf(SystemMessage(FULL_CONTEXT_PROMPT), UserMessage(transcript))))
        val text = response.result?.output?.text.orEmpty()
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start !in 0..<end) return null
        val scores = runCatching { JsonMapper.builder().build().readTree(text.substring(start, end + 1)).path("scores") }.getOrNull() ?: return null
        if (!ScoreDimension.entries.all { scores.path(it.key).isNumber }) return null
        return ScoreDimension.entries.sumOf { it.weight * scores.path(it.key).asDouble() } / ScoreDimension.TOTAL_WEIGHT
    }

    private val baselineModel: GoogleGenAiChatModel by lazy {
        chatModel(ScoreModelProperties(ScoreModelProperties().model, BASELINE_MAX_OUTPUT_TOKENS))
    }

    private fun spreadSubmission(messages: List<ChatMessage>): AnalysisSubmission {
        val now = Instant.now()
        val start = now.minus(Duration.ofDays(LONG_CHAT_DAYS))
        val step = Duration.ofDays(LONG_CHAT_DAYS).dividedBy(messages.size.toLong())
        val timed = messages.mapIndexed { index, source -> ChatMessage(source.speaker, start.plus(step.multipliedBy(index.toLong())), source.text) }
        return AnalysisSubmission(DataSource.KAKAO, ChatMessages(timed).eventsFor(SELF).selectedAt(now))
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
        private val SAMPLES = System.getenv("EVAL_SAMPLES")?.toIntOrNull() ?: 1
        private val LONG_REPETITIONS = System.getenv("EVAL_LONG_REPETITIONS")?.toIntOrNull() ?: 0
        private val VARIANT_REPETITIONS = System.getenv("EVAL_VARIANT_REPETITIONS")?.toIntOrNull() ?: 0
        private val VARIANT_NAMES = System.getenv("EVAL_VARIANT_NAMES")?.split(",") ?: listOf("half", "double", "group")
        private const val FULL_SELF_TURNS = 20
        private val SHRINKAGES = listOf(
            ScoreShrinkage(BigDecimal.ZERO, FULL_SELF_TURNS, personalPrior = false),
            ScoreShrinkage(BigDecimal("0.5"), FULL_SELF_TURNS, personalPrior = false),
            ScoreShrinkage(BigDecimal.ONE, FULL_SELF_TURNS, personalPrior = false),
            ScoreShrinkage(BigDecimal("0.25"), FULL_SELF_TURNS, personalPrior = false),
            ScoreShrinkage(BigDecimal("0.5"), 10, personalPrior = false),
            ScoreShrinkage(BigDecimal("0.5"), 5, personalPrior = false),
            ScoreShrinkage(BigDecimal("0.25"), 10, personalPrior = false),
            ScoreShrinkage(BigDecimal("0.25"), 5, personalPrior = false),
            ScoreShrinkage(BigDecimal("0.25"), 5, personalPrior = true),
        )
        private val DEFAULT_SHRINKAGE = ScoreShrinkageProperties().toShrinkage()
        private const val LENGTH_BASELINE_FILE = "build/eval/length-baseline.txt"
        private val BASELINE_REPETITIONS = System.getenv("EVAL_BASELINE_REPETITIONS")?.toIntOrNull() ?: 3
        private const val BASELINE_MAX_OUTPUT_TOKENS = 1024
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
        private const val PLAYFULNESS_SPLIT = 50
        private const val INPUT_USD_PER_TOKEN = 0.3 / 1_000_000
        private const val CACHED_INPUT_USD_PER_TOKEN = 0.03 / 1_000_000
        private const val OUTPUT_USD_PER_TOKEN = 2.5 / 1_000_000
        private val GOLDEN_DIR: String? = System.getenv("EVAL_GOLDEN_DIR")
        private const val GOLDEN_LABEL_PREFIX = "#labels"
        private const val GOLDEN_SPEARMAN_TARGET = 0.7
        private const val FULL_BASELINE_FILE = "build/eval/full-baseline.txt"
        private const val TEMPERATURE = 0.1
        private const val DEFAULT_MAX_OUTPUT_TOKENS = 250
        private const val JSON_MIME_TYPE = "application/json"
        private const val BLOCK_HEADER = "## "
        private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        private val PROMPT_ROW = Regex("'score',(\\d+),'((?:[^']++|'')*+)'\\)")
    }
}
