package com.santacut

import com.santacut.domain.analysis.model.GiftVerdict
import com.santacut.domain.dossier.model.DossierMetric
import com.santacut.domain.dossier.model.MetricReading
import com.santacut.domain.roast.model.BlockedWords
import com.santacut.domain.roast.model.ChipDirection
import com.santacut.domain.roast.model.LineQuote
import com.santacut.domain.roast.model.LineQuotes
import com.santacut.domain.roast.model.PhraseFactSpec
import com.santacut.domain.roast.model.RoastBrief
import com.santacut.domain.roast.model.RoastChip
import com.santacut.domain.roast.model.RoastFact
import com.santacut.domain.roast.model.RoastFacts
import com.santacut.domain.roast.model.RoastLines
import com.santacut.domain.roast.model.RoastQuotes
import com.santacut.domain.roast.model.RoastSlots
import com.santacut.domain.roast.model.RoastTopics
import com.santacut.domain.roast.model.ThanksFactSpec

/**
 * 리캡 5차 심사 회귀 테스트가 함께 쓰는 사실·입력 조립 도구. 사실은 실제 리소스(roast/facts.json, 조서 사전)의 규칙으로 만들고,
 * 말풍선은 합성 대화(pusher·roaster 페르소나)의 본인 말을 옮긴 것이다. 실제 대화는 쓰지 않는다.
 */
object TestRecapFacts {
    private val catalog = TestRoasts.catalog

    /** 석탄 덱 점수(49 이하, 석탄 당첨 구간). */
    const val COAL = 3569

    /** 석탄 덱 근거 카드 자리: 깎인 말 셋, 살아남은 말 하나. */
    val COAL_SLOTS = RoastSlots(0, 3, 1)

    /** 백분위 [percentile]에서 값 [value]를 읽은 지표 [key]의 사실. 백분위가 밋밋하면 null이다. */
    fun metricOrNull(key: String, value: Double, percentile: Double): RoastFact? {
        val spec = checkNotNull(catalog.facts.find(key)) { key }
        val metricSpec = TestRoasts.dossier.metrics.toList().first { candidate -> candidate.key == key }
        return spec.factOf(MetricReading(checkNotNull(DossierMetric.ofKey(key)), metricSpec, value, percentile))
    }

    fun metric(key: String, value: Double, percentile: Double): RoastFact = checkNotNull(metricOrNull(key, value, percentile)) { key }

    /** 입버릇 [key]('[display]') 사실. 방향은 facts.json의 praise·negative 목록을 따른다. */
    fun phrase(key: String, display: String, count: String = "43번"): RoastFact = RoastFact(
        PhraseFactSpec.KEY_PREFIX + key, "'$display'라고 보낸 횟수", "남들보다 훨씬 많음", catalog.phrase.group,
        RoastChip(catalog.phrase.chipLabel.replace(PhraseFactSpec.QUOTE, display), count), 0.8, direction = catalog.phrase.directionOf(key),
    )

    /** '고마워' 0번 사실. */
    fun thanks(): RoastFact = RoastFact(
        ThanksFactSpec.KEY, catalog.thanks.meaning, catalog.thanks.level, catalog.thanks.group, RoastChip("'고마워'", "0번"),
        direction = ChipDirection.BAD, dimension = catalog.thanks.dimension,
    )

    /** 본인 줄 [lines](번호는 1부터)와 사실 [facts]로 만든 작가 입력. */
    fun briefOf(hundredths: Int, facts: List<RoastFact>, lines: List<String> = emptyList(), slots: RoastSlots = COAL_SLOTS): RoastBrief {
        val verdict = GiftVerdict(hundredths)
        return RoastBrief(
            verdict, RoastFacts(facts), RoastQuotes.EMPTY, RoastTopics.EMPTY, RoastLines.EMPTY, BlockedWords.EMPTY,
            slang = catalog.rules.slang.allowed, slots = slots, lineQuotes = LineQuotes(lines.mapIndexed { index, text -> LineQuote(index + 1, text) }),
            statSlots = slots, phrases = TestRoasts.dossier.phrases, verdictPunch = catalog.punch.verdictFor(verdict, 0, catalog.rules),
            foreignMarks = catalog.punch.foreignMarksFor(verdict.grade()), examples = catalog.examples, thanksWords = catalog.thanks.words,
        )
    }

    /** 합성 pusher의 석탄 덱 사실(4차 실측 덱): 새벽 출몰률, '했어?', 말풍선 수, '빨리', '고마워' 0번. */
    fun pusherFacts(): List<RoastFact> = listOf(
        metric("night_ratio", 0.41, 99.0), phrase("haeteo_q", "했어?", "33번"), metric("message_count", 500.0, 20.0),
        phrase("ppalri", "빨리", "84번"), thanks(),
    )

    /** 합성 pusher의 본인 말풍선. */
    val PUSHER_LINES = listOf("아직도 안 옴?", "빨리 대답좀", "그래서 언제 됨?", "읽었으면 말을 해", "숙제 다 했어?")

    /** 합성 roaster의 석탄 덱 사실(4차 실측 덱): 따뜻한 말, '축하해', 하루 말풍선, ㅋ 밀도, '뭐임', '고마워' 0번. */
    fun roasterFacts(): List<RoastFact> = listOf(
        metric("kind_words_density", 16.0, 99.0), phrase("chukhahae", "축하해", "39번"), metric("messages_per_day", 2.1, 5.0),
        metric("laugh_density", 33.0, 20.0), phrase("mwoim", "뭐임", "43번"), thanks(),
    )

    /** 합성 roaster의 본인 말풍선. */
    val ROASTER_LINES = listOf("그 옷 뭐임 ㅋㅋㅋ", "아 진짜 노답이네", "축하는 해줄게", "잘했어 인정", "말은 잘하네")
}
