package com.santacut.domain.dossier.model

import kotlin.math.abs
import kotlin.math.ceil

/**
 * 한 사람의 극값 후보 지표 값과 시드 기준 백분위.
 */
class MetricReading(
    private val metric: DossierMetric,
    private val spec: MetricSpec,
    private val value: Double,
    private val percentile: Double,
) {
    val key: String = metric.key
    val family: MetricFamily = metric.family

    /** 백분위가 50에서 떨어진 정도. 클수록 그 사람에게서 튄다. */
    fun distance(): Double = abs(percentile - MIDDLE)

    /** 백분위가 50 이상(많은 쪽)인지. */
    fun isHigh(): Boolean = percentile >= MIDDLE

    fun isMockable(): Boolean = spec.mockable

    /** 느린 답장과 삐 처리는 카드에 싣지 않는다. 나머지는 공유할 수 있다. */
    fun isShareable(): Boolean = metric.isCardSafe(isHigh())

    /** 방향 key. 높은 쪽은 지표 key 그대로, 낮은 쪽은 `<key>_low`다(죄목·주석·칭호 문구의 key). */
    fun directionKey(): String = if (isHigh()) key else key + LOW_SUFFIX

    /** 지표 템플릿 슬롯. 값, 지표 이름, 상위·하위 몇 %급(1% 이상, 올림)과 지표마다 덧붙는 달·시간대다. */
    fun slots(stats: DossierStats): Slots = metric.extraSlots(stats) + Slots.of(
        VALUE to spec.format(value),
        LABEL to spec.label,
        TOP_PERCENT to DossierFormat.percent(ceil(MAX - percentile).coerceAtLeast(MIN_SHOWN)),
        BOTTOM_PERCENT to DossierFormat.percent(ceil(percentile).coerceAtLeast(MIN_SHOWN)),
    )

    /** [number]번째 극값 장. 놀림 문구를 쓸 수 없으면(mockable 거짓, 담백 톤) 지표 이름과 값에 중립 메모만 단다. */
    fun scene(number: Int, context: SceneContext): DossierScene {
        val copy = context.copy().extremes.of(key)
        val slots = context.slots + slots(context.stats)
        if (copy == null || !isMockable() || context.isMild()) return neutralScene(number, context, slots, copy?.speaker)
        val side = copy.side(isHigh())
        val seed = context.seed.of(EXTREME_PURPOSE + key)
        val line = side.lines.pick(seed.of(LINE_PURPOSE), slots)?.let { text -> LineDraft(text, isShareable()) }
        return DossierScene(
            DossierScene.EXTREME_ID_PREFIX + number, SceneKind.EXTREME, copy.speaker, side.headline.pick(seed.of(HEADLINE_PURPOSE), slots),
            slots.fillOrNull(side.big), SceneLines.numbered(listOfNotNull(line)), side.memo.pick(seed.of(MEMO_PURPOSE), slots),
            metric.chart?.chartOf(context.stats), isShareable(),
        )
    }

    private fun neutralScene(number: Int, context: SceneContext, slots: Slots, speaker: String?): DossierScene = DossierScene(
        DossierScene.EXTREME_ID_PREFIX + number, SceneKind.EXTREME, speaker ?: DossierScene.ELF, spec.label, spec.format(value),
        SceneLines.EMPTY, context.copy().mild.extremesMemo.pick(context.seed.of(MILD_PURPOSE + key), slots),
        metric.chart?.chartOf(context.stats), isShareable(),
    )

    companion object {
        const val LOW_SUFFIX = "_low"
        private const val MIDDLE = 50.0
        private const val MAX = 100.0
        private const val MIN_SHOWN = 1.0
        private const val VALUE = "value"
        private const val LABEL = "label"
        private const val TOP_PERCENT = "top_percent"
        private const val BOTTOM_PERCENT = "bottom_percent"
        private const val EXTREME_PURPOSE = "extreme."
        private const val MILD_PURPOSE = "extreme.mild."
        private const val HEADLINE_PURPOSE = "headline"
        private const val LINE_PURPOSE = "line"
        private const val MEMO_PURPOSE = "memo"
    }
}
