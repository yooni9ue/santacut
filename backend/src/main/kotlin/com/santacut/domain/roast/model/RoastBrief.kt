package com.santacut.domain.roast.model

import com.santacut.domain.analysis.model.GiftVerdict
import com.santacut.domain.analysis.model.ScoreComponents
import com.santacut.domain.analysis.model.ScoringMessage
import com.santacut.domain.dossier.model.DossierCatalog
import com.santacut.domain.dossier.model.DossierStats
import com.santacut.domain.dossier.model.KindWords
import com.santacut.domain.dossier.model.PhraseEntries
import tools.jackson.databind.ObjectMapper
import kotlin.math.abs

/**
 * 팩폭 작가 호출 하나의 서버 조립 입력. 판정, 사실(칩 후보), 인용 후보, 화제 후보, 가린 본인 말풍선, 근거 카드 슬롯 수와 그 요청의 차단 목록,
 * 근거로 고를 수 있는 본인 줄의 말풍선 표시를 함께 쥔다. 차단 목록과 말풍선 표시는 검증과 저장할 근거 카드에만 쓰고 작가에게 보내지 않는다.
 *
 * @property slots 서버가 정한 근거 카드 수(항목 점수로 정하고 재료로 줄인 것)
 * @property statSlots 칩 통계 카드로 채울 때의 근거 카드 수. 말풍선이 필요 없어 칩 후보 수로만 줄인다.
 * @property phrases 입버릇 사전. 입버릇 칩을 고른 말풍선에 그 입버릇이 있는지 셀 때 쓴다(집계와 같은 표면형으로 센다).
 * @property kinds 따뜻한 말 사전. 따뜻한 말 칩을 고른 말풍선에 따뜻한 말이 있는지 볼 때 쓴다([QuoteRule.KIND]).
 * @property peakHour 본인 말풍선이 가장 많이 몰린 시. 피크타임 칩의 말풍선 조건([QuoteRule.PEAK_HOUR])이다. 모르면 null이다.
 * @property punches 결론 마지막 한 줄 펀치 틀 후보(시드 순서). 앞 [suggestedPunches]가 작가 추천이고, 나머지는 대체 문장 예비다.
 * @property suggestedPunches 작가에게 정해 준 펀치 틀. 마지막 한 줄은 이 가운데 하나를 써야 한다. 비었으면 따지지 않는다.
 * @property verdictPunch 작가에게 정해 준 판정 한 줄 틀(유행어 가족). 판정 한 줄은 이 틀의 표지를 써야 한다. null이면 따지지 않는다.
 * @property foreignMarks 이 사람의 구간이 쓸 수 없는 다른 구간의 표현(구간 이름, 구간 밈)
 * @property examples 그대로 베끼면 떨어지는 예시 문장(프롬프트 예시, 대체 문장)
 * @property thanksWords 고맙다는 말의 줄기. 고마워 칩이 있으면 판정 한 줄에 쓰지 못한다.
 */
class RoastBrief(
    val verdict: GiftVerdict,
    val facts: RoastFacts,
    private val quotes: RoastQuotes,
    private val topics: RoastTopics,
    private val lines: RoastLines,
    private val blocked: BlockedWords,
    private val slang: SlangWords = SlangWords.EMPTY,
    val slots: RoastSlots = RoastSlots.NONE,
    private val lineQuotes: LineQuotes = LineQuotes.EMPTY,
    private val statSlots: RoastSlots = slots,
    private val phrases: PhraseEntries = PhraseEntries.EMPTY,
    private val kinds: KindWords? = null,
    private val peakHour: Int? = null,
    private val punches: PunchTemplates = PunchTemplates.EMPTY,
    val suggestedPunches: PunchTemplates = PunchTemplates.EMPTY,
    val verdictPunch: PunchTemplate? = null,
    private val foreignMarks: ForeignMarks = ForeignMarks.EMPTY,
    private val examples: ExampleLines = ExampleLines.EMPTY,
    private val thanksWords: List<String> = emptyList(),
) {
    /** 작가 입력. 다시 쓰기면 [rewrite]를 싣는다. */
    fun input(rewrite: RoastRewrite? = null): RoastInput =
        RoastInput(
            verdict.key(), verdict.margin().word, verdict.grade().name, verdict.grade().temperature, facts, facts.chipHints(peakHour), slots,
            quotes, topics, lines.toActs(), slang.hints(), rewrite, suggestedPunches.hints(), verdictPunch?.template, nearMiss(),
            signatureKey(), foreignMarks.toList(),
        )

    /**
     * 컷과 1점 안으로 붙었을 때 작가에게 주는 말("컷 바로 위 0.13점 차"). 아슬아슬함을 판정 한 줄·결론에 살리게 한다. 1점 이상 떨어졌으면 null이다.
     */
    fun nearMiss(): String? {
        val distance = verdict.cutDistanceHundredths()
        if (abs(distance) >= NEAR_MISS_HUNDREDTHS) return null
        val side = if (verdict.passed) NEAR_ABOVE else NEAR_BELOW
        return side + verdict.cutDistanceText().drop(1) + NEAR_SUFFIX
    }

    /** [text]에 쓴 다른 구간의 표현. 없으면 null이다. */
    fun foreignMarkIn(text: String): String? = foreignMarks.foundIn(text)

    /** [text]가 그대로 베낀 예시 문장인지. */
    fun isExample(text: String): Boolean = text in examples

    /** 고마워 칩이 있을 때 [text]에 쓴 고맙다는 말. 고마워 0번이 판정 한 줄을 이끌지 못하게 한다. 칩이 없거나 말이 없으면 null이다. */
    fun thanksWordIn(text: String): String? {
        if (!hasFact(ThanksFactSpec.KEY)) return null
        val bare = TextProblems.QUOTE.replace(text, "")
        return thanksWords.firstOrNull { word -> word in bare }
    }

    /** 입버릇 낱말(칩 입버릇과 인용 후보). 판정 한 줄의 판정 자리를 입버릇 하나로 채웠는지 볼 때 쓴다. */
    fun phraseTokens(): Set<String> = (facts.phrases() + quotes.toList()).toSet()

    /** [key] 칩 후보의 방향. 칩 후보가 아니면 null이다. */
    fun directionOf(key: String): ChipDirection? = facts.find(key)?.direction

    /** [key] 칩 후보의 영역 태그용 항목 점수 key. 없으면 null이다. */
    fun dimensionOf(key: String): String? = facts.find(key)?.dimension

    /**
     * 대표 사실 칩 key. 근거 카드나 결론 대표 칩 가운데 한 곳에 써야 한다. 가장 튀는 근거용 사실([RoastFacts.signature])이 작가가 실제로 놓을 수 있는
     * 칩일 때만 대표로 정한다: 말풍선 없이 쓰는 칩이거나, 그 칩의 말풍선 조건에 맞는 본인 줄이 있거나, 깎인 말 자리가 있고 깎인 말에 쓸 수 있는 칩이다.
     * 놓을 수 없으면 작가가 통과할 길이 없어 null이다.
     */
    fun signatureKey(): String? {
        val fact = facts.signature() ?: return null
        val quoteless = quoteRuleOf(fact.key) == QuoteRule.NONE
        val fitting = lineQuotes.usableIds().any { lineId -> quoteFits(fact.key, lineId) }
        val cuttable = fact.direction.allows(EvidenceKind.CUT) && slots.cut > 0
        return fact.key.takeIf { quoteless || fitting || cuttable }
    }

    /** 작가 사용자 메시지 본문(JSON 한 줄). 테스트·실측 도구에서도 이 함수로 입력을 만든다. */
    fun inputText(objectMapper: ObjectMapper, rewrite: RoastRewrite? = null): String = objectMapper.writeValueAsString(input(rewrite))

    fun hasFact(key: String): Boolean = facts.find(key) != null

    /**
     * [key] 칩 후보의 칩 이름, 뜻 풀이, 버릇 낱말([RoastFact.topics]). 일반론 검사의 근거로 쓴다(고마워 칩 뜻 풀이는 고맙다·감사 같은 변형을 든다).
     * 버릇 낱말이 있어 한 줄이 칩 글자를 그대로 옮기지 않고 뜻으로 말해도 통과한다. 없으면 빈 목록이다.
     */
    fun chipTextsOf(key: String): List<String> = facts.find(key)?.let { fact -> listOf(fact.chip.label, fact.meaning) + fact.topics }.orEmpty()

    /**
     * [key] 칩이 말하는 시간대 글(칩 이름, 칩 값, 버릇 낱말). 근거 한 줄의 시간대 낱말은 이 글이나 고른 말풍선에 있어야 한다.
     * 칩 후보가 아니면 빈 목록이다.
     */
    fun chipTimeTextsOf(key: String): List<String> = facts.find(key)?.let { fact -> listOf(fact.chip.label, fact.chip.value) + fact.topics }.orEmpty()

    /** [key] 칩이 칩 이름을 그대로 옮기면 칩 되풀이가 되는 지표 칩인지(입버릇·고마워 칩이 아니고, 이름이 버릇 낱말에 들지 않는다). */
    fun isRestatable(key: String): Boolean {
        val fact = facts.find(key) ?: return false
        return !key.startsWith(PhraseFactSpec.KEY_PREFIX) && key != ThanksFactSpec.KEY && fact.chip.label !in fact.topics
    }

    /** [key]가 입버릇 칩 후보면 그 입버릇 표기. 입버릇 칩이 아니거나 후보가 없으면 null이다. */
    fun phraseOf(key: String): String? = chipOf(key)?.takeIf { key.startsWith(PhraseFactSpec.KEY_PREFIX) }?.quotedPhrase()

    /**
     * 말풍선 표시 [quote]에 [key] 입버릇 칩의 입버릇이 있는지. 칩 횟수를 센 것과 같은 사전 표면형으로 센다(미쳤다는 미쳤·ㅁㅊ, 말도 안 돼는 말도안돼도 친다).
     * 입버릇 칩이 아니면 참이다. 사전에서 찾지 못한 입버릇은 칩 표기가 글자 그대로 있어야 한다.
     */
    fun quoteHasPhrase(key: String, quote: String): Boolean {
        val display = phraseOf(key) ?: return true
        val entry = phrases.find(key.removePrefix(PhraseFactSpec.KEY_PREFIX)) ?: return display in quote
        return entry.countIn(quote) > 0
    }

    /**
     * [key] 칩 후보의 말풍선 조건. 칩 후보가 아니면 말풍선 없이 쓰는 [QuoteRule.NONE]이다.
     * 피크타임 조건인데 가장 바쁜 시를 모르면(조서 집계 없음) 맞는 말풍선을 가릴 수 없어 [QuoteRule.NONE]으로 본다.
     */
    fun quoteRuleOf(key: String): QuoteRule {
        val rule = facts.find(key)?.quoteRule ?: return QuoteRule.NONE
        return if (rule == QuoteRule.PEAK_HOUR && peakHour == null) QuoteRule.NONE else rule
    }

    /**
     * [lineId]번 말풍선이 [key] 칩의 말풍선 조건([QuoteRule])에 맞는지. 입버릇 칩은 [quoteHasPhrase]로 본다.
     * 보낸 본인 줄이 아니면 거짓이다. 칩과 말풍선이 다른 이야기를 하지 않게 한다('언제 가능?'에 피크타임 칩).
     */
    fun quoteFits(key: String, lineId: Int): Boolean {
        val quote = lineQuotes.lineOf(lineId) ?: return false
        val rule = quoteRuleOf(key)
        return if (rule == QuoteRule.PHRASE) quoteHasPhrase(key, quote.text) else rule.accepts(quote, peakHour, kinds)
    }

    /** 근거로 고를 수 있는(말풍선이 넉넉한) 본인 줄 수. */
    fun usableLineCount(): Int = lineQuotes.usableCount()

    /** 판정 한 줄 [verdictLine]과 펀치가 겹치지 않는 대체 마지막 한 줄. [plain]이면 유행어 없는 문장만 고른다. 후보가 없으면 null이다. */
    fun punchFallback(verdictLine: String, plain: Boolean, rules: RoastRules): String? = punches.fallbackFor(verdictLine, plain, rules)

    /** [key] 칩 후보의 서버 칩 값. 없으면 null이다. */
    fun chipOf(key: String): RoastChip? = facts.find(key)?.chip

    /** [lineId]번 본인 줄의 말풍선 표시. 작가에게 보낸 본인 줄이 아니면 null이다. 상대 줄은 보내지 않으므로 늘 null이다. */
    fun quoteOf(lineId: Int): String? = lineQuotes.find(lineId)

    /**
     * 칩 후보로 만든 통계 카드. 작가 근거가 검증을 끝내 통과하지 못한 자리에 쓴다. 이미 받은 근거 [kept]가 있으면
     * 통계 카드 슬롯 수에서 그 수를 뺀 만큼만, [kept]가 쓰지 않은 칩으로 만든다. 점수 깎인 말은 고마워 칩을 먼저 쓴다.
     */
    fun fallbackEvidence(fallback: RoastFallback, kept: RoastEvidences = RoastEvidences.EMPTY): RoastEvidences =
        facts.chipFacts().fallbackEvidence(statSlots.remainingAfter(kept), fallback, kept)

    /**
     * 작은따옴표로 인용해도 되는지. 인용 후보 목록에 있거나, 목록 밖이라도 웃음·울음 자모 표기([RoastQuotes.isLaughOrCry])가
     * 본인 말풍선에 글자 그대로 있으면 된다. 작가가 목록에 없는 'ㅋㅋㅋ'를 자주 써 다시 쓰기만 늘렸고, 본인이 실제로 친 표기라 사실과도 맞다.
     */
    fun isQuotable(quote: String): Boolean = quote in quotes || (RoastQuotes.isLaughOrCry(quote) && lines.containsText(quote))

    /** [text]가 본인 말풍선(가린 뒤)에서 [COPIED_LENGTH]자 이상 그대로 베낀 첫 구간. 따옴표 없이 옮겨 쓴 인용을 막는다. 없으면 null이다. */
    fun copiedIn(text: String): String? = lines.sharedRun(text, COPIED_LENGTH)

    /** [text]에 나온 차단 낱말(입력에서 가린 사람 이름·사는 곳 이름·영문). 없으면 null이다. */
    fun blockedIn(text: String): String? = blocked.foundIn(text)

    /** 작가에게 보내는 줄 수. 로그에는 이 수만 남긴다. */
    fun lineCount(): Int = lines.size()

    companion object {
        /** 막 하나에 보내는 본인 말풍선 글자 수 상한. 세 막 합이 약 6천 자(입력 약 4~5천 토큰)다. */
        const val CHARS_PER_ACT = 2_000

        /** 이만큼 이어진 글자가 본인 말풍선과 같으면 베낀 것으로 본다. */
        const val COPIED_LENGTH = 8

        /** 컷과 이만큼(100분의 1점) 안으로 붙으면 아슬아슬하다는 말을 작가에게 준다. */
        private const val NEAR_MISS_HUNDREDTHS = 100
        private const val NEAR_ABOVE = "컷 바로 위 "
        private const val NEAR_BELOW = "컷 바로 아래 "
        private const val NEAR_SUFFIX = "점 차"

        /**
         * 메시지 [messages](본인과 상대, 시각순, 한국 시각)와 조서 집계 [stats], 선물 판정 [verdict], 항목 점수 [dimensions]로 입력을 조립한다.
         * 본인 줄만 쓰고, 사람 이름·사는 곳 이름·숫자·영문·링크는 가린다. 지명·가게·행사 이름은 그대로 둔다. [stats]가 없으면(조서 집계 실패) 사실·인용·화제 없이 보낸다.
         * 입버릇은 지금 사전으로 다시 읽은 집계를 쓴다. 테스트·실측 도구에서도 이 함수로 입력을 만든다.
         * 대화 참여자 이름 [participants](원본 파일 제출만 있음)는 모든 줄에서 가리고 차단 목록에 넣는다.
         * 근거 카드 슬롯 수는 항목 점수로 정하고, 말풍선 표시가 남는 본인 줄 수와 방향별 칩 수로 줄인다([RoastSlots.limitedTo]).
         * 판정 한 줄 틀과 마지막 한 줄 틀은 [seed]로 하나씩 정해 준다([RoastPunch]).
         * [seed]는 사실 고르기와 펀치 틀 추천의 시드다([RoastFacts.of], [RoastPunch.candidatesFor]). 같은 참가자·같은 해면 같은 값을 넘겨
         * 재시도에도 같은 사실·같은 추천 펀치가 나오게 하고, 다른 사람에게는 다른 펀치를 추천한다.
         */
        fun compose(
            messages: List<ScoringMessage>,
            stats: DossierStats?,
            verdict: GiftVerdict,
            dimensions: ScoreComponents,
            catalog: RoastCatalog,
            dossier: DossierCatalog,
            participants: ParticipantNames = ParticipantNames.EMPTY,
            seed: Int = 0,
        ): RoastBrief {
            val all = RoastLines.of(messages, catalog.rules, participants)
            val sent = all.trimmed(CHARS_PER_ACT)
            val lineQuotes = sent.quotes(catalog.rules.quotePattern(all.quoteWords() + participants.toList()))
            val refreshed = stats?.refreshedBy(dossier.phrases)
            val facts = refreshed?.let { counted -> RoastFacts.of(counted, dossier, catalog, seed) } ?: RoastFacts.EMPTY
            val slots = RoastSlots.of(verdict, dimensions)
            val punches = catalog.punch.candidatesFor(verdict, seed)
            val supply = facts.supply()
            return RoastBrief(
                verdict,
                facts,
                refreshed?.let { counted -> RoastQuotes.of(counted, catalog.rules) } ?: RoastQuotes.EMPTY,
                refreshed?.let { counted -> RoastTopics.of(counted, dossier.topics, catalog.rules.excludedTopics) } ?: RoastTopics.EMPTY,
                sent,
                all.blockedWords().plus(participants.toList()),
                catalog.rules.slang.allowed,
                slots.limitedTo(lineQuotes.usableCount(), supply),
                lineQuotes,
                slots.limitedTo(Int.MAX_VALUE, supply),
                dossier.phrases,
                dossier.kinds,
                refreshed?.let { counted -> counted.clock.peakHour },
                punches,
                punches.take(catalog.punch.suggestions),
                catalog.punch.verdictFor(verdict, seed, catalog.rules),
                catalog.punch.foreignMarksFor(verdict.grade()),
                catalog.examples,
                catalog.thanks.words,
            )
        }
    }
}
