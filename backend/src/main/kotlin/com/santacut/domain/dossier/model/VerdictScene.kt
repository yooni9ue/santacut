package com.santacut.domain.dossier.model

/**
 * 판결 장(산타). 판정은 기존 점수 판정 그대로, 사유는 4개 항목 중 최고점 항목의 따뜻한 말 횟수, 주석은 통계 하나다.
 * 큰 값은 전국 순위, 메모는 지역과 도착 예정 시각이다. 하위 구간 줄은 카드에서 가린다.
 */
class VerdictScene(private val context: SceneContext, private val extremes: MetricReadings, private val subject: DossierSubject) {
    private val copy = context.copy().verdict

    fun scene(): DossierScene = DossierScene(
        VERDICT, SceneKind.VERDICT, DossierScene.SANTA, subject.verdictTitle, subject.rank?.let(DossierFormat::nationalRank),
        SceneLines.numbered(listOfNotNull(reason(), note(), lowBand())), delivery(), null, true,
    )

    /** 최고점 항목에서 가장 많이 쓴 따뜻한 말과 그 횟수. 그 항목의 말을 한 번도 안 썼으면 뺀다. */
    private fun reason(): LineDraft? {
        val dimension = subject.topDimension()
        val kind = context.catalog.kinds.strongestIn(dimension, context.stats.words.kindWords) ?: return null
        val slots = context.slots + Slots.of(KIND_LABEL to kind.label, COUNT to DossierFormat.times(kind.count))
        return copy.reasonByDimension.of(dimension.key)?.pick(context.seed.of(REASON), slots)?.let { text -> LineDraft(text, true) }
    }

    /** ㅋ 연타, 극값 지표, 나머지 주석 key 순서로 쓸 수 있는 첫 주석. 없으면 default 주석이다. 담백 톤이면 뺀다. */
    private fun note(): LineDraft? {
        if (context.isMild()) return null
        return (StatKeys(listOf(LAUGH_MAX_RUN)) + extremes.directionKeys() + copy.note.keys())
            .render(copy.note, context, NOTE, 1, emptySet(), true).firstDraft()
    }

    /** 하위 판정 줄. 화면 전용이고 담백 톤이면 뺀다. */
    private fun lowBand(): LineDraft? {
        if (!subject.band().isLow() || context.isMild()) return null
        return copy.lowBandTitle.pick(context.seed.of(LOW_BAND), context.slots)?.let { text -> LineDraft(text, false) }
    }

    /** 지역과 도착 예정 시각. 둘 다 없으면 null이다. */
    private fun delivery(): String? {
        val parts = listOfNotNull(subject.regionName, subject.eta?.let { eta -> DELIVERY_PREFIX + DossierFormat.eta(eta) })
        return parts.joinToString(SEPARATOR).ifEmpty { null }
    }

    companion object {
        private const val VERDICT = "verdict"
        private const val REASON = "verdict.reason"
        private const val NOTE = "verdict.note."
        private const val LOW_BAND = "verdict.low_band"
        private const val LAUGH_MAX_RUN = "laugh_max_run"
        private const val KIND_LABEL = "kind_label"
        private const val COUNT = "count"
        private const val DELIVERY_PREFIX = "도착 예정 "
        private const val SEPARATOR = " · "
    }
}
