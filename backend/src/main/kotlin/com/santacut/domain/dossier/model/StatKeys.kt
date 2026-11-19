package com.santacut.domain.dossier.model

/**
 * 문구를 채울 통계 key 후보. 앞에 있을수록 먼저 고른다. 겹친 key는 한 번만 본다.
 */
class StatKeys(private val values: List<String>) {
    operator fun plus(other: List<String>): StatKeys = StatKeys(values + other)

    /**
     * key마다 [templates]에 문구가 있고 값이 있는(채울 수 있는) 것만 [limit]개까지 채운다.
     * 통계 key(방향 접미사를 뗀 것)가 [excluded]에 있거나 이미 고른 것과 같으면 건너뛴다.
     * 템플릿은 [purpose]와 key를 이은 시드로 고른다. [strict]는 [SceneContext.slotsFor]의 방향 규칙이다.
     */
    fun render(templates: KeyedTemplates, context: SceneContext, purpose: String, limit: Int, excluded: Set<String>, strict: Boolean): RenderedStats =
        RenderedStats(
            values.distinct().asSequence()
                .mapNotNull { key -> renderOne(key, templates, context, purpose, strict) }
                .filterNot { stat -> stat.statKey() in excluded }
                .distinctBy(RenderedStat::statKey)
                .take(limit)
                .toList(),
        )

    private fun renderOne(key: String, templates: KeyedTemplates, context: SceneContext, purpose: String, strict: Boolean): RenderedStat? {
        val candidates = templates.of(key) ?: return null
        val slots = context.slotsFor(key, strict) ?: return null
        return candidates.pick(context.seed.of(purpose + key), slots)?.let { text -> RenderedStat(key, text, context.isCardSafe(key)) }
    }
}
