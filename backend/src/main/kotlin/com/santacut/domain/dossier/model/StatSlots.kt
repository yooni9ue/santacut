package com.santacut.domain.dossier.model

/**
 * 모든 템플릿이 함께 쓰는 공통 슬롯. 집계값을 서버가 단위까지 포맷해 넣는다. 값이 없는 슬롯은 빠지고, 그 슬롯을 쓴 문구는 고르지 않는다.
 * ㅋ 연타는 [NOTE_MIN_RUN]연타 이상일 때만 넣는다. 짧은 연타로 주석을 달지 않으려는 것이다.
 */
object StatSlots {
    const val NOTE_MIN_RUN = 10
    private const val PIECES = "개"
    private const val CHARACTERS = "자"
    private const val DAYS = "일"
    private const val TIMES = "배"
    private const val RUN = "연타"
    private const val PERCENT = 100.0
    private const val CASE_NUMBER_START = 1000
    private const val CASE_NUMBER_RANGE = 9000
    private const val CASE_PURPOSE = "case"

    /** 본인 메시지 수 표기. 티저는 이 값을 가려 쓴다. */
    fun messageCountText(stats: DossierStats): String = DossierFormat.amount(stats.messageCount().toDouble(), PIECES)

    fun laughCountText(stats: DossierStats): String = DossierFormat.amount(stats.laugh.laughCount.toDouble(), PIECES)

    /** 조서 번호. 해와 결과 ID 시드로 정한 네 자리 수를 잇는다. */
    fun caseNumber(seed: TemplateSeed, year: Int): String =
        "$year-${seed.of(CASE_PURPOSE).indexFor(CASE_NUMBER_RANGE) + CASE_NUMBER_START}"

    fun of(stats: DossierStats, subject: DossierSubject, seed: TemplateSeed, title: String): Slots =
        volume(stats) + laugh(stats) + clock(stats) + style(stats) + Slots.of(
            "title" to title, "verdict" to subject.verdictTitle, "rank" to subject.rank?.let(DossierFormat::rank),
            "case_no" to caseNumber(seed, subject.caseYear),
        )

    private fun volume(stats: DossierStats): Slots = Slots.of(
        "message_count" to messageCountText(stats),
        "char_count" to DossierFormat.amount(stats.volume.charCount.toDouble(), CHARACTERS),
        "active_days" to DossierFormat.amount(stats.volume.activeDays.toDouble(), DAYS),
        "busiest_day_count" to stats.volume.busiestDay?.let { day -> DossierFormat.amount(day.count.toDouble(), PIECES) },
        "busiest_day_month" to stats.volume.busiestDay?.let { day -> DossierFormat.month(day.month) },
        "bleep_count" to stats.words.bleepCount.takeIf { count -> count > 0 }?.let(DossierFormat::times),
    )

    private fun laugh(stats: DossierStats): Slots {
        val run = stats.laugh.laughMaxRun?.takeIf { longest -> longest.length >= NOTE_MIN_RUN }
        val emoji = stats.emoji.top.top()
        return Slots.of(
            "laugh_count" to laughCountText(stats),
            "laugh_density" to DossierFormat.amount(stats.laugh.laughDensity, PIECES),
            "laugh_max_run" to run?.let { longest -> DossierFormat.amount(longest.length.toDouble(), RUN) },
            "laugh_max_run_month" to run?.let { longest -> DossierFormat.month(longest.month) },
            "laugh_max_run_hour" to run?.let { longest -> DossierFormat.hour(longest.hour) },
            "emoji" to emoji?.emoji, "emoji_count" to emoji?.let { top -> DossierFormat.times(top.count) },
        )
    }

    private fun clock(stats: DossierStats): Slots = Slots.of(
        "peak_hour" to stats.clock.peakHour?.let(DossierFormat::hour),
        "night_ratio" to DossierFormat.percent(stats.clock.nightRatio * PERCENT),
        "busiest_month" to stats.clock.busiestMonth?.let(DossierFormat::month),
        "quietest_month" to stats.clock.quietestMonth?.let(DossierFormat::month),
        "month_ratio" to stats.clock.monthRatio?.let { ratio -> DossierFormat.decimal(ratio, TIMES) },
        "reply_median" to stats.reply.medianMinutes?.let(DossierFormat::minutes),
    )

    private fun style(stats: DossierStats): Slots = Slots.of(
        "avg_length" to DossierFormat.decimal(stats.style.avgLength, CHARACTERS),
        "short_ratio" to DossierFormat.percent(stats.style.shortRatio * PERCENT),
        "burst_avg" to DossierFormat.decimal(stats.style.burstAvg, PIECES),
        "burst_max" to DossierFormat.amount(stats.style.burstMax.toDouble(), PIECES),
        "question_ratio" to DossierFormat.percent(stats.style.questionRatio * PERCENT),
        "exclaim_ratio" to DossierFormat.percent(stats.style.exclaimRatio * PERCENT),
        "longest_message_length" to DossierFormat.amount(stats.style.longestMessageLength.toDouble(), CHARACTERS),
    )
}
