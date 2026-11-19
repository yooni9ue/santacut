package com.santacut.domain.dossier.model

import com.santacut.domain.analysis.model.ScoringMessage
import java.time.LocalDate
import kotlin.math.roundToLong

/**
 * 조서를 셀 본인 메시지 전체(시각순, 한국 시각). 상대 메시지는 담지 않는다.
 * 글자 집계(웃음, 이모지, 길이)는 원래 본문으로, 사전 집계(입버릇, 주제, 따뜻한 말, 욕설)는 정규화한 본문으로 센다.
 * 요일·달·활동일은 하루 경계 05:00으로 센다.
 */
class SelfMessages(private val values: List<ScoringMessage>) {
    fun size(): Int = values.size

    fun volume(): VolumeStats = VolumeStats(
        values.size, values.sumOf { message -> message.text.length }, values.map(::dayOf).distinct().size, busiestDay(),
    )

    fun laugh(): LaughStats {
        val laughs = values.sumOf { message -> charactersOf(message.text, LAUGH_CHARACTER) }
        val cries = values.sumOf { message -> charactersOf(message.text, CRY_CHARACTER) }
        return LaughStats(laughs, density(laughs), longestLaughRun(), cries, density(cries))
    }

    fun emoji(): EmojiStats {
        val emojis = values.flatMap { message -> Graphemes.of(message.text).emojis() }
        return EmojiStats(EmojiTallies.of(emojis), density(emojis.size))
    }

    fun clock(): ClockStats {
        val hourly = Counts.of(values.map { message -> message.localTime.hour }, HOURS)
        val monthly = Counts.of(values.map { message -> dayOf(message).monthValue - 1 }, MONTHS)
        val weekday = Counts.of(values.map { message -> dayOf(message).dayOfWeek.ordinal }, WEEKDAYS)
        val busiest = monthly.peakIndex()
        val quietest = monthly.quietestIndex()
        return ClockStats(
            hourly.toList(), monthly.toList(), weekday.toList(), hourly.peakIndex(), ratio(hourly.totalIn(NIGHT_HOURS), values.size),
            busiest?.plus(1), quietest?.plus(1), monthRatio(monthly, busiest, quietest),
        )
    }

    /** 본인 연속 메시지 묶음 크기 [bursts]를 함께 받아 메시지 모양을 센다. */
    fun style(bursts: Counts): MessageStyle {
        val texts = values.filterNot(::isPlaceholderOrBlank).map { message -> message.text.trim() }
        val burstAverage = if (bursts.size() == 0) 0.0 else roundToTenth(bursts.total().toDouble() / bursts.size())
        return MessageStyle(
            roundToTenth(texts.map { text -> text.length }.average().takeUnless(Double::isNaN) ?: 0.0),
            ratio(texts.count { text -> text.length <= SHORT_LENGTH }, texts.size), burstAverage, bursts.max(),
            ratio(texts.count { text -> QUESTION in text }, texts.size), ratio(texts.count { text -> EXCLAIM in text }, texts.size),
            texts.maxOfOrNull { text -> text.length } ?: 0,
        )
    }

    /** 사전으로 센 낱말 집계. 주제는 막마다 따로 센다. */
    fun words(catalog: DossierCatalog): WordStats {
        val text = joinedText()
        val hits = catalog.phrases.hitsIn(text)
        val actTexts = DossierAct.entries.map { act -> inMonths(act.months).joinedText() }
        return WordStats(
            hits.frequent(), hits.distinct(values.size), catalog.kinds.countIn(text), catalog.topics.countByAct(actTexts),
            catalog.bleep.countIn(text),
        )
    }

    /** 자리표시와 빈 메시지를 뺀 정규화 본문을 줄바꿈으로 이은 글. 사전 집계에 쓴다. */
    fun joinedText(): String = values.filterNot(::isPlaceholderOrBlank).joinToString(LINE_BREAK) { message -> message.normalizedText() }

    private fun inMonths(months: IntRange): SelfMessages = SelfMessages(values.filter { message -> dayOf(message).monthValue in months })

    private fun busiestDay(): BusiestDay? = values.groupBy(::dayOf).maxByOrNull { (_, messages) -> messages.size }
        ?.let { (day, messages) -> BusiestDay(messages.size, day.monthValue) }

    private fun longestLaughRun(): LaughRun? {
        val message = values.maxByOrNull { candidate -> longestRunIn(candidate.text) } ?: return null
        val length = longestRunIn(message.text)
        return if (length == 0) null else LaughRun(length, message.localTime.monthValue, message.localTime.hour)
    }

    private fun longestRunIn(text: String): Int = LAUGH_RUN.findAll(text).maxOfOrNull { run -> run.value.length } ?: 0

    private fun monthRatio(monthly: Counts, busiest: Int?, quietest: Int?): Double? {
        if (busiest == null || quietest == null) return null
        return roundToTenth(monthly[busiest].toDouble() / monthly[quietest])
    }

    private fun density(count: Int): Double = if (values.isEmpty()) 0.0 else roundToTenth(count * PER_HUNDRED / values.size)

    /** 0~1 사이 비율(소수 셋째 자리). */
    private fun ratio(part: Int, whole: Int): Double = if (whole == 0) 0.0 else (part * THOUSANDTHS / whole).roundToLong() / THOUSANDTHS

    private fun charactersOf(text: String, pattern: Regex): Int = text.length - text.replace(pattern, "").length

    private fun isPlaceholderOrBlank(message: ScoringMessage): Boolean = message.isPlaceholder() || message.text.isBlank()

    private fun dayOf(message: ScoringMessage): LocalDate = message.localTime.minusHours(DAY_BOUNDARY_HOURS).toLocalDate()

    private fun roundToTenth(value: Double): Double = (value * TENTHS).roundToLong() / TENTHS

    companion object {
        /** 이 시각 전 메시지는 전날로 센다. */
        const val DAY_BOUNDARY_HOURS = 5L
        const val SHORT_LENGTH = 5
        private const val HOURS = 24
        private const val MONTHS = 12
        private const val WEEKDAYS = 7

        /** 0시부터 5시 전까지. */
        private val NIGHT_HOURS = 0..4
        private const val PER_HUNDRED = 100.0
        private const val TENTHS = 10.0
        private const val THOUSANDTHS = 1000.0
        private const val QUESTION = '?'
        private const val EXCLAIM = '!'
        private const val LINE_BREAK = "\n"
        private val LAUGH_CHARACTER = Regex("[ㅋㅎ]")
        private val CRY_CHARACTER = Regex("[ㅠㅜ]")
        private val LAUGH_RUN = Regex("ㅋ+")
    }
}
