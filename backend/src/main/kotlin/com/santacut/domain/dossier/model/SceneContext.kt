package com.santacut.domain.dossier.model

/**
 * 장면을 조립할 때 함께 쓰는 값. 사전, 집계값, 톤, 노출 구간, 템플릿 시드, 공통 슬롯, 읽을 수 있는 지표, 칭호다.
 */
class SceneContext(
    val catalog: DossierCatalog,
    val stats: DossierStats,
    private val tone: DossierTone,
    val exposure: DossierExposure,
    val seed: TemplateSeed,
    val slots: Slots,
    val readings: MetricReadings,
    val title: String,
) {
    fun copy(): DossierCopy = catalog.copy

    fun isMild(): Boolean = tone.isMild()

    /**
     * 통계 key 하나(죄목·공로·주석)로 문구를 채울 슬롯. 값이 없어 쓸 수 없으면 null이다.
     * 지표 key는 극값 후보 조건(최소 메시지 수, 1:1 답장)을 넘어야 하고, `<key>_low`는 낮은 쪽일 때만 쓴다.
     * [strict]이면 접미사 없는 지표 key는 높은 쪽일 때만 쓴다(죄목·주석·칭호의 방향 규칙).
     * phrase_top은 많이 쓴 말 1위, phrase_distinct는 특이도 1위, busiest_month는 가장 붐빈 달, 따뜻한 말 key는 한 번 이상 쓴 말이다.
     */
    fun slotsFor(key: String, strict: Boolean): Slots? {
        val metricKey = key.removeSuffix(MetricReading.LOW_SUFFIX)
        if (DossierMetric.ofKey(metricKey) != null) return metricSlots(key, metricKey, strict)
        return when (key) {
            PHRASE_TOP -> stats.words.phrasesFrequent.at(1)?.let { phrase -> slots + phrase.slots() }
            PHRASE_DISTINCT -> stats.words.phrasesDistinct.at(1)?.let { phrase -> slots + phrase.slots() }
            BUSIEST_MONTH -> busiestMonthSlots()
            else -> kindSlots(key)
        }
    }

    /** [key] 문구를 공유 카드에 실어도 되는지. 느린 답장과 삐 처리 지표만 막는다. */
    fun isCardSafe(key: String): Boolean = readings.find(key.removeSuffix(MetricReading.LOW_SUFFIX))?.isShareable() ?: true

    private fun metricSlots(key: String, metricKey: String, strict: Boolean): Slots? {
        val reading = readings.find(metricKey) ?: return null
        val wantsLow = key != metricKey
        if (wantsLow == reading.isHigh() && (wantsLow || strict)) return null
        return slots + reading.slots(stats)
    }

    private fun busiestMonthSlots(): Slots? {
        val month = stats.clock.busiestMonth ?: return null
        val count = stats.clock.monthly.getOrNull(month - 1) ?: return null
        return slots + Slots.of(MONTH to DossierFormat.month(month), VALUE to DossierFormat.amount(count.toDouble(), PIECES))
    }

    /** 따뜻한 말 key면 그 이름과 횟수, 사전에 없는 key(default 등)면 공통 슬롯이다. */
    private fun kindSlots(key: String): Slots? {
        val kindLabel = catalog.kinds.labelOf(key) ?: return slots
        val count = stats.words.kindWords.countOf(key)
        if (count == 0) return null
        return slots + Slots.of(LABEL to kindLabel, KIND_LABEL to kindLabel, COUNT to DossierFormat.times(count))
    }

    companion object {
        const val PHRASE_TOP = "phrase_top"
        const val PHRASE_DISTINCT = "phrase_distinct"
        const val BUSIEST_MONTH = "busiest_month"
        private const val LABEL = "label"
        private const val KIND_LABEL = "kind_label"
        private const val COUNT = "count"
        private const val MONTH = "month"
        private const val VALUE = "value"
        private const val PIECES = "개"
    }
}
