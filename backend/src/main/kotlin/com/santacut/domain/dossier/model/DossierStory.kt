package com.santacut.domain.dossier.model

/**
 * 저장한 집계값과 사전, 기존 결과로 조회 시점에 조서 장면 목록을 조립한다. 문구 선택은 결과 ID 시드로 결정적이다.
 *
 * 장면 순서는 티저, 올해의 한마디, 입버릇 장부, 극값(최대 3), 3막, 산타 조서, 판결, 소환장이다.
 * 본인 메시지 수 구간([DossierExposure])에 따라 장부·3막·조서를 빼거나 줄인다.
 * 칭호는 극값 1위 지표 방향의 수식어와 극값 2위 지표 방향의 직함을 잇는다. 놀릴 수 없는 지표나 담백 톤이면 기본 부품을 쓴다.
 */
class DossierStory(
    private val catalog: DossierCatalog,
    private val stats: DossierStats,
    private val tone: DossierTone,
    private val subject: DossierSubject,
) {
    private val seed = TemplateSeed(subject.assessmentId)
    private val exposure = DossierExposure.of(stats.messageCount())
    private val readings = catalog.metrics.readings(stats)
    private val extremes = readings.extremes(exposure.extremeCount)

    fun write(): DossierView {
        val title = title()
        val context = SceneContext(catalog, stats, tone, exposure, seed, StatSlots.of(stats, subject, seed, title), readings, title)
        val opening = OpeningScenes(context)
        val closing = ClosingScenes(context, extremes, subject)
        val scenes = listOfNotNull(opening.teaser(), opening.hook(), opening.ledger()) + extremes.toScenes(context) +
            listOfNotNull(closing.acts(), closing.caseFile(), VerdictScene(context, extremes, subject).scene(), closing.summons())
        return DossierView(title, tone, DossierScenes(scenes))
    }

    private fun title(): String {
        val titles = catalog.copy.titles
        val modifier = titlePart(titles.modifierByMetric, extremes.at(0), titles.defaultModifier, MODIFIER)
        val role = titlePart(titles.roleByMetric, extremes.at(1), titles.defaultRole, ROLE)
        return listOfNotNull(modifier, role).joinToString(SPACE)
    }

    private fun titlePart(byMetric: KeyedTemplates, reading: MetricReading?, fallback: Templates, purpose: String): String? {
        val partSeed = seed.of(purpose)
        val usable = reading?.takeIf { candidate -> candidate.isMockable() && !tone.isMild() }
        return usable?.let { candidate -> byMetric.of(candidate.directionKey()) }?.pick(partSeed, Slots.EMPTY)
            ?: fallback.pick(partSeed, Slots.EMPTY)
    }

    companion object {
        private const val MODIFIER = "title.modifier"
        private const val ROLE = "title.role"
        private const val SPACE = " "
    }
}
