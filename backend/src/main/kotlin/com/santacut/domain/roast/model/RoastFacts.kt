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
 * 맨 앞은 근거용 사실 가운데 S가 가장 큰 사실이고(같으면 시드로 정한다), 나머지는 아직 안 나온 계열의 근거용 사실(없으면 중립 사실) 가운데 softmax(S / [TEMPERATURE]) 무게로
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
        RoastChipFacts(values.map { fact -> RoastChipFact(fact.key, fact.chip, fact.direction, fact.dimension, fact.topic()) })

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

    /** 근거 카드에 쓸 수 있는 칩 수를 방향별로 센 값. 근거 카드 슬롯 수를 재료로 줄일 때 쓴다([RoastSlots.limitedTo]). */
    fun supply(): ChipSupply = ChipSupply(
        values.count { fact -> fact.direction == ChipDirection.GOOD },
        values.count { fact -> fact.direction == ChipDirection.BAD },
        values.count { fact -> fact.direction == ChipDirection.EITHER },
    )

    /** 칩 후보 가운데 입버릇 칩의 입버릇 표기. 판정 한 줄이 입버릇 하나만으로 판정 자리를 채웠는지 볼 때 쓴다. */
    fun phrases(): List<String> = values.mapNotNull { fact -> fact.chip.quotedPhrase()?.takeIf { fact.key.startsWith(PhraseFactSpec.KEY_PREFIX) } }

    /**
     * 다른 깎인 말 칩만으로 깎인 말 자리가 다 차면 고마워 칩을 뺀 후보. '고마워' 0번은 구체적인 나쁜 버릇 칩이 모자랄 때만 깎인 말로 쓴다.
     * 자리 수는 서버 슬롯 [slots]를 본인 말 [lineCount]줄과 방향별 칩 수로 줄인 값([RoastSlots.limitedTo])으로 견준다. 고마워 칩이 없으면 그대로다.
     */
    fun withoutSpareThanks(slots: RoastSlots, lineCount: Int): RoastFacts {
        val others = RoastFacts(values.filterNot { fact -> fact.key == ThanksFactSpec.KEY })
        if (others.size() == size()) return this
        val spare = slots.limitedTo(lineCount, others.supply()).cut >= slots.limitedTo(lineCount, supply()).cut
        return if (spare) others else this
    }

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

    /**
     * 특이도 순으로 줄 세운 입버릇 후보에서 반드시 실을 입버릇. 가장 튀는 입버릇(대표 입버릇)과 깎인 말에 쓸 수 있는 가장 튀는 입버릇이다.
     * 깎인 말 입버릇은 나쁜 쪽('빨리')을 먼저, 없으면 어느 쪽으로도 쓰는 것('뭐임', '했어?')을 고른다. 둘이 같으면 하나다.
     * 시드 뽑기가 '빨리' 84번 같은 재촉 입버릇을 빼 석탄 덱이 본인 말 카드를 잃던 문제를 막는다.
     */
    fun requiredPhrases(): RoastFacts {
        val top = values.firstOrNull() ?: return EMPTY
        val cuttable = values.firstOrNull { fact -> fact.direction == ChipDirection.BAD } ?: values.firstOrNull { fact -> fact.direction == ChipDirection.EITHER }
        return RoastFacts(listOfNotNull(top, cuttable).distinct())
    }

    /** 앞 [count]개. */
    fun take(count: Int): RoastFacts = RoastFacts(values.take(count))

    /** [other]에 든 사실을 뺀 나머지. */
    fun without(other: RoastFacts): RoastFacts = RoastFacts(values.filterNot { fact -> fact in other.values })

    /**
     * 남은 계열에서 하나씩 더 뽑는다. 근거 카드에 쓸 수 있는 사실을 먼저 뽑고, 그런 사실이 남은 계열에 없을 때만 중립 사실(대표 칩 후보)을 뽑는다.
     * [limit]는 근거용 사실 수로만 센다. 중립 사실이 뽑기 몫과 계열을 먼저 차지해 새벽 출몰률 같은 근거용 사실이 빠지지 않게 한다.
     */
    private tailrec fun drawRest(picked: List<RoastFact>, random: Random, limit: Int): List<RoastFact> {
        val usedGroups = picked.map(RoastFact::group).toSet()
        val open = values.filterNot { fact -> fact.group in usedGroups }
        if (picked.count { fact -> fact.direction.isEvidence() } >= limit || open.isEmpty()) return picked
        val evidence = open.filter { fact -> fact.direction.isEvidence() }
        return drawRest(picked + weightedPick(evidence.ifEmpty { open }, random), random, limit)
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

        /** 계열이 겹치지 않게 뽑는 근거용 사실 수 상한(입버릇 하나 포함, 중립 사실은 세지 않는다). 덧붙는 입버릇까지 근거용 사실은 최대 [MAX_DRAWN_FACTS] + 1개다. */
        const val MAX_DRAWN_FACTS = 8

        /** softmax 온도. 작을수록 점수 큰 사실만 나오고, 클수록 고르게 섞인다. */
        const val TEMPERATURE = 0.2

        /** 입버릇은 점수가 큰 [PHRASE_POOL]개 가운데 [MAX_PHRASE_FACTS]개까지 시드로 고른다. 반드시 실을 입버릇을 더해도 [MAX_PHRASES]개까지다. */
        private const val PHRASE_POOL = 5
        private const val MAX_PHRASE_FACTS = 2
        private const val MAX_PHRASES = 3

        /**
         * 조서 집계 [stats](지금 사전으로 다시 읽은 것)에서 사실을 고른다. 지표는 놀릴 수 있고(mockable) 카드에 실을 수 있고 값이 0이 아니며
         * 백분위가 가운데가 아닌 것만 후보다. 입버릇은 인용해도 되는 말 가운데 보통보다 튀는 것만 후보다.
         * 입버릇은 점수 무게로 뽑고, 반드시 실을 것([requiredPhrases]: 가장 튀는 입버릇, 깎인 말에 쓸 가장 튀는 입버릇)을 [MAX_PHRASES]개 안에서 더한다.
         * 고르기는 [seed](같은 참가자·같은 해면 같은 값)와 이 방의 집계를 섞은 난수로 한다. 재시도에는 같은 사실이, 다른 사람·다른 방에는 다른 사실이 나온다.
         * 고른 입버릇 가운데 앞의 것은 지표와 함께 앞자리를 다투고, 뽑기에 들지 못한 입버릇은 모두 맨 뒤에 덧붙는다. 고맙다는 말이 한 번도 없으면 그 사실을 맨 뒤에 덧붙인다.
         */
        fun of(stats: DossierStats, dossier: DossierCatalog, catalog: RoastCatalog, seed: Int = 0): RoastFacts {
            val random = Random(Objects.hash(seed, stats.volume.messageCount, stats.volume.charCount, stats.volume.activeDays))
            val pool = phraseFacts(stats, dossier, catalog)
            val required = pool.requiredPhrases()
            val sampled = pool.sampled(random, PHRASE_POOL, MAX_PHRASE_FACTS)
            val phrases = (required + sampled.without(required)).take(MAX_PHRASES)
            val drawn = (metricFacts(dossier.metrics.readings(stats).toList(), catalog.facts) + sampled.head()).drawn(random, MAX_DRAWN_FACTS)
            return drawn + phrases.without(drawn) + RoastFacts(listOfNotNull(catalog.thanks.factOf(stats)))
        }

        private fun metricFacts(readings: List<MetricReading>, specs: RoastFactSpecs): RoastFacts = RoastFacts(
            readings
                .filter { reading -> reading.isMockable() && reading.isShareable() && !reading.isZero() }
                .mapNotNull { reading -> specs.find(reading.key)?.factOf(reading) },
        )

        /** 입버릇 사실 후보. 특이도(보통 사람보다 몇 배 튀는지, 점수 상한 없이)가 큰 순서다. 같은 점수로 묶인 입버릇 가운데 대표 입버릇을 가린다. */
        private fun phraseFacts(stats: DossierStats, dossier: DossierCatalog, catalog: RoastCatalog): RoastFacts = RoastFacts(
            (stats.words.phrasesDistinct.toList() + stats.words.phrasesFrequent.toList())
                .distinctBy(PhraseTally::key)
                .filter { tally -> catalog.rules.isQuotable(tally.key, tally.display) }
                .mapNotNull { tally -> dossier.phrases.find(tally.key)?.let { entry -> tally to entry } }
                .sortedByDescending { (tally, entry) -> entry.specificity(tally.count, stats.textCount()) }
                .mapNotNull { (tally, entry) -> catalog.phrase.factOf(tally, entry, stats.textCount()) },
        )
    }
}
