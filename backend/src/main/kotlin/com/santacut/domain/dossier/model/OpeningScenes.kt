package com.santacut.domain.dossier.model

/**
 * 조서 앞부분 장면: 티저, 올해의 한마디, 입버릇 장부. 화자는 모두 계수 요정이다.
 */
class OpeningScenes(private val context: SceneContext) {
    private val copy = context.copy()
    private val words = context.stats.words

    /** 가린 숫자만 흘리는 티저. 메시지가 적으면 더 시끄러운 방을 올리라는 줄을 맨 앞에 둔다. 공유하지 않는다. */
    fun teaser(): DossierScene {
        val masked = Slots.of(
            MESSAGE_COUNT to DossierFormat.masked(StatSlots.messageCountText(context.stats)),
            LAUGH_COUNT to DossierFormat.masked(StatSlots.laughCountText(context.stats)),
        )
        val lowData = if (context.exposure.isSparse()) copy.lowData.pick(context.seed.of(LOW_DATA), context.slots) else null
        val teaser = copy.teaser.pick(context.seed.of(TEASER), context.slots + masked)
        val lines = listOfNotNull(lowData?.let { text -> LineDraft(text, false) }, teaser?.let { text -> LineDraft(text, false) })
        return DossierScene(TEASER, SceneKind.TEASER, DossierScene.ELF, null, null, SceneLines.numbered(lines), null, null, false)
    }

    /** 특이도 1위 말. 최소 횟수([HOOK_MIN_COUNT])에 못 미치면 ㅋ 밀도로 바꾼다. */
    fun hook(): DossierScene {
        val phrase = words.phrasesDistinct.at(1)?.takeIf { top -> top.count >= HOOK_MIN_COUNT } ?: return laughHook()
        val slots = context.slots + phrase.slots()
        return DossierScene(
            HOOK, SceneKind.HOOK, DossierScene.ELF, copy.hook.headline.pick(context.seed.of(HOOK_HEADLINE), slots), phrase.display,
            SceneLines.numbered(listOf(LineDraft(DossierFormat.times(phrase.count), true))),
            copy.hook.memo.pick(context.seed.of(HOOK_MEMO), slots), null, true,
        )
    }

    /** 많이 쓴 말과 삐 처리 횟수. 삐 처리 줄은 화면 전용이다. 쓸 말도 욕설도 없거나 메시지가 적으면 장을 뺀다. */
    fun ledger(): DossierScene? {
        val size = context.exposure.ledgerSize
        if (size == 0 || (words.phrasesFrequent.isEmpty() && words.bleepCount == 0)) return null
        val bleep = copy.ledger.bleep.pick(context.seed.of(LEDGER_BLEEP), context.slots)?.let { text -> LineDraft(text, false) }
        return DossierScene(
            LEDGER, SceneKind.LEDGER, DossierScene.ELF, copy.ledger.headline.pick(context.seed.of(LEDGER_HEADLINE), context.slots), null,
            SceneLines.numbered(words.phrasesFrequent.ledgerLines(size) + listOfNotNull(bleep)), ledgerMemo(), null, true,
        )
    }

    /** 대체형. 큰 값과 {value}는 ㅋ 밀도를 metrics.json의 단위로 포맷한 값이다. */
    private fun laughHook(): DossierScene {
        val density = context.stats.laugh.laughDensity
        val value = context.catalog.metrics.format(LAUGH_DENSITY, density) ?: DossierFormat.decimal(density, PIECES)
        val slots = context.slots + Slots.of(VALUE to value)
        return DossierScene(
            HOOK, SceneKind.HOOK, DossierScene.ELF, copy.hookFallback.headline.pick(context.seed.of(HOOK_HEADLINE), slots), value,
            SceneLines.EMPTY, copy.hookFallback.memo.pick(context.seed.of(HOOK_MEMO), slots), null, true,
        )
    }

    /** 1위 말의 분류에 맞는 요정 메모. */
    private fun ledgerMemo(): String? {
        val top = words.phrasesFrequent.at(1) ?: return null
        return copy.ledger.memoByCategory.of(top.category)?.pick(context.seed.of(LEDGER_MEMO), context.slots + top.slots())
    }

    companion object {
        /** 올해의 한마디가 되려면 필요한 최소 횟수(잠정). */
        const val HOOK_MIN_COUNT = 20
        private const val TEASER = "teaser"
        private const val HOOK = "hook"
        private const val LEDGER = "ledger"
        private const val LOW_DATA = "low_data"
        private const val HOOK_HEADLINE = "hook.headline"
        private const val HOOK_MEMO = "hook.memo"
        private const val LEDGER_HEADLINE = "ledger.headline"
        private const val LEDGER_MEMO = "ledger.memo"
        private const val LEDGER_BLEEP = "ledger.bleep"
        private const val MESSAGE_COUNT = "message_count"
        private const val LAUGH_COUNT = "laugh_count"
        private const val PIECES = "개"
        private const val VALUE = "value"
        private const val LAUGH_DENSITY = "laugh_density"
    }
}
