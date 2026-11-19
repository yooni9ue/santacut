package com.santacut.domain.dossier.model

/**
 * 조서 뒷부분 장면: 「올해의 너 3막」(루돌프), 산타 조서와 소환장(산타).
 */
class ClosingScenes(private val context: SceneContext, private val extremes: MetricReadings, private val subject: DossierSubject) {
    private val copy = context.copy()

    /** 막별 화제 1위. 메시지가 적으면 빼고, 보여 줄 막이 하나도 없으면 뺀다. */
    fun acts(): DossierScene? {
        if (!context.exposure.showsActs) return null
        val lines = context.catalog.topics.actLines(context)
        if (lines.isEmpty()) return null
        return DossierScene(
            ACTS, SceneKind.ACTS, DossierScene.RUDOLPH, copy.acts.headline.pick(context.seed.of(ACTS), context.slots), null,
            SceneLines.numbered(lines), null, ChartKind.MONTHLY.chartOf(context.stats), true,
        )
    }

    /** 칭호, 죄목 2, 공로 2, 총평. 죄목과 공로는 서로 다른 통계를 쓴다. 담백 톤이면 죄목을 빼고 총평도 담백 문구로 바꾼다. */
    fun caseFile(): DossierScene? {
        if (!context.exposure.showsCaseFile) return null
        val charges = if (context.isMild()) RenderedStats(emptyList()) else charges()
        val merits = merits(charges.statKeys())
        return DossierScene(
            DOSSIER, SceneKind.DOSSIER, DossierScene.SANTA, copy.dossier.header.pick(context.seed.of(HEADER), context.slots), context.title,
            SceneLines.numbered(charges.drafts() + merits.drafts() + listOfNotNull(summary())), null, null, true,
        )
    }

    /** 공유 도발 문구. 특이도 1위 말(없으면 많이 쓴 말 1위)을 슬롯으로 쓴다. 담백 톤이면 죄목·놀림 없는 중립 문구를 쓴다. */
    fun summons(): DossierScene {
        val words = context.stats.words
        val phrase = words.phrasesDistinct.at(1) ?: words.phrasesFrequent.at(1)
        val slots = context.slots + (phrase?.slots() ?: Slots.EMPTY)
        val templates = if (context.isMild()) copy.mild.summons else copy.summons
        return DossierScene(
            SUMMONS, SceneKind.SUMMONS, DossierScene.SANTA, null, context.title,
            SceneLines.numbered(templates.pickLines(context.seed.of(SUMMONS), slots, SUMMONS_LIMIT, true)), null, null, true,
        )
    }

    /** 극값으로 뽑힌 지표, 특이도·빈도 1위 입버릇, 나머지 지표 순서로 죄목을 고른다. 방향 key(_low)를 지킨다. */
    private fun charges(): RenderedStats =
        (StatKeys(extremes.mockableDirectionKeys()) + listOf(SceneContext.PHRASE_DISTINCT, SceneContext.PHRASE_TOP) +
            context.readings.mockableDirectionKeys())
            .render(copy.dossier.charges, context, CHARGE, CHARGE_LIMIT, emptySet(), true)

    /** 많이 쓴 따뜻한 말, 가장 붐빈 달, 극값 지표, 나머지 공로 문구 순서로 고른다. 죄목에 쓴 통계([chargeKeys])는 건너뛴다. */
    private fun merits(chargeKeys: Set<String>): RenderedStats =
        (StatKeys(context.catalog.kinds.keysByCount(context.stats.words.kindWords)) + listOf(SceneContext.BUSIEST_MONTH) +
            extremes.directionKeys() + copy.dossier.merits.keys())
            .render(copy.dossier.merits, context, MERIT, MERIT_LIMIT, chargeKeys, false)

    private fun summary(): LineDraft? {
        val templates = if (context.isMild()) copy.mild.summary else copy.dossier.summaryByBand.of(subject.band().key)
        return templates?.pick(context.seed.of(SUMMARY), context.slots)?.let { text -> LineDraft(text, true) }
    }

    companion object {
        const val CHARGE_LIMIT = 2
        const val MERIT_LIMIT = 2
        const val SUMMONS_LIMIT = 3
        private const val ACTS = "acts"
        private const val DOSSIER = "dossier"
        private const val SUMMONS = "summons"
        private const val HEADER = "dossier.header"
        private const val CHARGE = "dossier.charge."
        private const val MERIT = "dossier.merit."
        private const val SUMMARY = "dossier.summary"
    }
}
