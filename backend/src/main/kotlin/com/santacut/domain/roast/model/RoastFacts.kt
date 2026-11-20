package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonValue
import com.santacut.domain.dossier.model.DossierCatalog
import com.santacut.domain.dossier.model.DossierStats
import com.santacut.domain.dossier.model.MetricReading
import com.santacut.domain.dossier.model.PhraseTally

/**
 * 작가에게 보내는 사실 목록(6~8개 목표). 입버릇 사실을 앞에, 튀는 지표 사실을 뒤에 둔다. JSON에서는 배열 하나로 나간다.
 */
class RoastFacts(private val values: List<RoastFact>) {
    @JsonValue
    fun toList(): List<RoastFact> = values.toList()

    fun keys(): List<String> = values.map(RoastFact::key)

    fun size(): Int = values.size

    fun find(key: String): RoastFact? = values.find { fact -> fact.key == key }

    /** [usedKeys]에 없는 사실마다 대체 짝을 만든다. 대체 문장이 없는 사실은 건너뛴다. */
    fun fallbackPairsExcept(usedKeys: List<String>, fallback: RoastFallback): List<RoastPair> =
        values.filterNot { fact -> fact.key in usedKeys }.mapNotNull(fallback::pairFor)

    companion object {
        val EMPTY = RoastFacts(emptyList())

        /** 입버릇 사실은 [MAX_PHRASE_FACTS]개까지, 지표 사실은 계열이 겹치지 않게 [MAX_METRIC_FACTS]개까지다. */
        private const val MAX_PHRASE_FACTS = 2
        private const val MAX_METRIC_FACTS = 6

        /**
         * 조서 집계 [stats](지금 사전으로 다시 읽은 것)에서 사실을 고른다. 지표는 놀릴 수 있고(mockable) 카드에 실을 수 있고 값이 0이 아니며
         * 백분위가 가운데가 아닌 것만, 50에서 먼 순으로 계열마다 하나씩 고른다. 입버릇은 인용해도 되는 말 가운데 보통보다 튀는 것만 고른다.
         */
        fun of(stats: DossierStats, dossier: DossierCatalog, catalog: RoastCatalog): RoastFacts =
            RoastFacts(phraseFacts(stats, dossier, catalog) + metricFacts(dossier.metrics.readings(stats).toList(), catalog.facts))

        private fun metricFacts(readings: List<MetricReading>, specs: RoastFactSpecs): List<RoastFact> = readings
            .filter { reading -> reading.isMockable() && reading.isShareable() && !reading.isZero() }
            .sortedByDescending(MetricReading::distance)
            .mapNotNull { reading -> specs.find(reading.key)?.factOf(reading) }
            .distinctBy(RoastFact::group)
            .take(MAX_METRIC_FACTS)

        private fun phraseFacts(stats: DossierStats, dossier: DossierCatalog, catalog: RoastCatalog): List<RoastFact> =
            (stats.words.phrasesDistinct.toList() + stats.words.phrasesFrequent.toList())
                .distinctBy(PhraseTally::key)
                .filter { tally -> catalog.rules.isQuotable(tally.key, tally.display) }
                .mapNotNull { tally -> dossier.phrases.find(tally.key)?.let { entry -> catalog.phrase.factOf(tally, entry, stats.textCount()) } }
                .take(MAX_PHRASE_FACTS)
    }
}
