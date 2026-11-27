package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonValue
import com.santacut.domain.dossier.model.DossierCatalog
import com.santacut.domain.dossier.model.DossierStats
import com.santacut.domain.dossier.model.MetricReading
import com.santacut.domain.dossier.model.PhraseTally
import java.util.Objects
import kotlin.math.exp
import kotlin.random.Random

/**
 * 작가에게 보내는 사실 목록(8~10개 목표). 앞자리일수록 세게 튀는 사실이고, 앞의 세 사실은 계열이 모두 다르다. JSON에서는 배열 하나로 나간다.
 *
 * 고르는 법: 사실마다 점수 S = 튀는 정도(백분위가 50에서 먼 정도, 0~1) × 편집 무게(facts.json의 weight)를 매긴다.
 * 맨 앞은 S가 가장 큰 사실이고(같으면 시드로 정한다), 나머지는 아직 안 나온 계열의 사실 가운데 softmax(S / [TEMPERATURE]) 무게로
 * 시드 뽑기를 되풀이한다. 같은 시드면 늘 같은 사실이 같은 순서로 나온다.
 */
class RoastFacts(private val values: List<RoastFact>) {
    @JsonValue
    fun toList(): List<RoastFact> = values.toList()

    fun keys(): List<String> = values.map(RoastFact::key)

    fun size(): Int = values.size

    fun find(key: String): RoastFact? = values.find { fact -> fact.key == key }

    /** 근거 카드 칩 후보. 순서는 사실 순서다. */
    fun chipFacts(): RoastChipFacts =
        RoastChipFacts(values.map { fact -> RoastChipFact(fact.key, fact.chip, fact.direction, fact.dimension) })

    /**
     * 작가 입력의 칩 후보와 말풍선 조건. 칩마다 쓸 수 있는 근거 종류(kinds)를 방향([ChipDirection.kindKeys])으로 싣는다.
     * 고마워 칩은 나쁜 쪽이라 점수 깎인 말 후보로만 나가고, 중립 칩은 kinds가 비어 결론 대표 칩으로만 쓴다. [peakHour]는 피크타임 칩 조건에 넣는 가장 바쁜 시다.
     */
    fun chipHints(peakHour: Int? = null): RoastChipHints = RoastChipHints(
        values.map { fact ->
            RoastChipHint(fact.key, fact.chip.label, fact.chip.value, fact.quoteRule.hintWith(peakHour), fact.direction.kindKeys())
        },
    )

    /**
     * 이 사람을 가장 잘 말하는 대표 사실(signature). 근거 카드에 쓸 수 있는(중립이 아닌) 사실 가운데 맨 앞이다. [drawn]이 시드로 섞은 뒤
     * 점수(튀는 정도)가 가장 큰 것을 맨 앞에 두므로 가장 튀는 버릇이다. 고마워 칩은 대표 사실이 아니다. 없으면 null이다.
     */
    fun signature(): RoastFact? = values.firstOrNull { fact -> fact.direction.isEvidence() && fact.key != ThanksFactSpec.KEY }

    /** [kind] 근거 카드에 쓸 수 있는 칩 수. 근거 카드 슬롯 수를 재료로 줄일 때 쓴다. */
    fun countFor(kind: EvidenceKind): Int = values.count { fact -> fact.direction.allows(kind) }

    /** 근거 카드에 쓸 수 있는(중립이 아닌) 칩 수. */
    fun evidenceCount(): Int = values.count { fact -> fact.direction.isEvidence() }

    /** 칩 후보 가운데 입버릇 칩의 입버릇 표기. 판정 한 줄이 입버릇 하나만으로 판정 자리를 채웠는지 볼 때 쓴다. */
    fun phrases(): List<String> = values.mapNotNull { fact -> fact.chip.quotedPhrase()?.takeIf { fact.key.startsWith(PhraseFactSpec.KEY_PREFIX) } }

    /** 이 후보들에 [other]를 덧붙인다. */
    operator fun plus(other: RoastFacts): RoastFacts = RoastFacts(values + other.values)

    /**
     * 후보에서 계열이 겹치지 않게 [limit]개까지 뽑는다. 맨 앞은 근거 카드에 쓸 수 있는 사실 가운데 점수가 가장 큰 것(대표 사실, 같은 점수끼리는
     * [random]이 섞은 순서)이고, 그런 사실이 없을 때만 중립 사실 가운데 가장 큰 것이다. 그 뒤는 남은 계열의 사실 가운데 softmax(점수 / [TEMPERATURE]) 무게로 하나씩 뽑는다.
     */
    fun drawn(random: Random, limit: Int): RoastFacts {
        val shuffled = values.shuffled(random)
        val first = shuffled.filter { fact -> fact.direction.isEvidence() }.maxByOrNull(RoastFact::score)
            ?: shuffled.maxByOrNull(RoastFact::score) ?: return EMPTY
        return RoastFacts(drawRest(listOf(first), random, limit))
    }

    /**
     * 점수가 큰 [pool]개 가운데 softmax(점수 / [TEMPERATURE]) 무게로 [count]개까지 겹치지 않게 뽑는다.
     * 센 입버릇이 자주 나오되 늘 같은 말만 나오지는 않게 한다.
     */
    fun sampled(random: Random, pool: Int, count: Int): RoastFacts =
        RoastFacts(sampleFrom(values.sortedByDescending(RoastFact::score).take(pool), emptyList(), random, count))

    /** 맨 앞 사실 하나만 든 목록. 비었으면 빈 목록이다. */
    fun head(): RoastFacts = RoastFacts(values.take(1))

    /** 맨 앞을 뺀 나머지. */
    fun tail(): RoastFacts = RoastFacts(values.drop(1))

    private tailrec fun drawRest(picked: List<RoastFact>, random: Random, limit: Int): List<RoastFact> {
        val usedGroups = picked.map(RoastFact::group).toSet()
        val open = values.filterNot { fact -> fact.group in usedGroups }
        if (picked.size >= limit || open.isEmpty()) return picked
        return drawRest(picked + weightedPick(open, random), random, limit)
    }

    private tailrec fun sampleFrom(open: List<RoastFact>, picked: List<RoastFact>, random: Random, count: Int): List<RoastFact> {
        if (picked.size >= count || open.isEmpty()) return picked
        val next = weightedPick(open, random)
        return sampleFrom(open - next, picked + next, random, count)
    }

    /** [open]에서 softmax(점수 / [TEMPERATURE]) 무게로 하나를 뽑는다. */
    private fun weightedPick(open: List<RoastFact>, random: Random): RoastFact {
        val cumulative = open.map { fact -> exp(fact.score / TEMPERATURE) }.runningReduce(Double::plus)
        val target = random.nextDouble() * cumulative.last()
        val index = cumulative.indexOfFirst { bound -> bound > target }
        return open[if (index < 0) open.lastIndex else index]
    }

    companion object {
        val EMPTY = RoastFacts(emptyList())

        /** 계열이 겹치지 않게 뽑는 사실 수 상한(입버릇 하나 포함). 덧붙는 입버릇까지 최대 [MAX_DRAWN_FACTS] + 1개다. */
        const val MAX_DRAWN_FACTS = 8

        /** softmax 온도. 작을수록 점수 큰 사실만 나오고, 클수록 고르게 섞인다. */
        const val TEMPERATURE = 0.2

        /** 입버릇은 점수가 큰 [PHRASE_POOL]개 가운데 [MAX_PHRASE_FACTS]개까지 시드로 고른다. */
        private const val PHRASE_POOL = 5
        private const val MAX_PHRASE_FACTS = 2

        /**
         * 조서 집계 [stats](지금 사전으로 다시 읽은 것)에서 사실을 고른다. 지표는 놀릴 수 있고(mockable) 카드에 실을 수 있고 값이 0이 아니며
         * 백분위가 가운데가 아닌 것만 후보다. 입버릇은 인용해도 되는 말 가운데 보통보다 튀는 것만 후보다.
         * 입버릇도 점수 무게로 뽑는다. 고르기는 [seed](같은 참가자·같은 해면 같은 값)와 이 방의 집계를 섞은 난수로 한다. 재시도에는 같은 사실이, 다른 사람·다른 방에는 다른 사실이 나온다.
         * 고른 입버릇 가운데 앞의 것은 지표와 함께 앞자리를 다투고, 둘째 것은 맨 뒤에 덧붙는다. 고맙다는 말이 한 번도 없으면 그 사실을 맨 뒤에 덧붙인다.
         */
        fun of(stats: DossierStats, dossier: DossierCatalog, catalog: RoastCatalog, seed: Int = 0): RoastFacts {
            val random = Random(Objects.hash(seed, stats.volume.messageCount, stats.volume.charCount, stats.volume.activeDays))
            val phrases = phraseFacts(stats, dossier, catalog).sampled(random, PHRASE_POOL, MAX_PHRASE_FACTS)
            val drawn = (metricFacts(dossier.metrics.readings(stats).toList(), catalog.facts) + phrases.head()).drawn(random, MAX_DRAWN_FACTS)
            return drawn + phrases.tail() + RoastFacts(listOfNotNull(catalog.thanks.factOf(stats)))
        }

        private fun metricFacts(readings: List<MetricReading>, specs: RoastFactSpecs): RoastFacts = RoastFacts(
            readings
                .filter { reading -> reading.isMockable() && reading.isShareable() && !reading.isZero() }
                .mapNotNull { reading -> specs.find(reading.key)?.factOf(reading) },
        )

        private fun phraseFacts(stats: DossierStats, dossier: DossierCatalog, catalog: RoastCatalog): RoastFacts = RoastFacts(
            (stats.words.phrasesDistinct.toList() + stats.words.phrasesFrequent.toList())
                .distinctBy(PhraseTally::key)
                .filter { tally -> catalog.rules.isQuotable(tally.key, tally.display) }
                .mapNotNull { tally -> dossier.phrases.find(tally.key)?.let { entry -> catalog.phrase.factOf(tally, entry, stats.textCount()) } },
        )
    }
}
