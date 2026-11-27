package com.santacut.domain.roast.model

import com.santacut.domain.analysis.model.GiftVerdict
import com.santacut.domain.analysis.model.ScoreComponents
import com.santacut.domain.analysis.model.ScoringMessage
import com.santacut.domain.dossier.model.DossierCatalog
import com.santacut.domain.dossier.model.DossierStats
import tools.jackson.databind.ObjectMapper

/**
 * 팩폭 작가 호출 하나의 서버 조립 입력. 판정, 사실(칩 후보), 인용 후보, 화제 후보, 가린 본인 말풍선, 근거 카드 슬롯 수와 그 요청의 차단 목록,
 * 근거로 고를 수 있는 본인 줄의 말풍선 표시를 함께 쥔다. 차단 목록과 말풍선 표시는 검증과 저장할 근거 카드에만 쓰고 작가에게 보내지 않는다.
 *
 * @property slots 서버가 정한 근거 카드 수(항목 점수로 정하고 재료로 줄인 것)
 * @property statSlots 칩 통계 카드로 채울 때의 근거 카드 수. 말풍선이 필요 없어 칩 후보 수로만 줄인다.
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
) {
    /** 작가 입력. 다시 쓰기면 [rewrite]를 싣는다. */
    fun input(rewrite: RoastRewrite? = null): RoastInput =
        RoastInput(
            verdict.key(), verdict.margin().word, verdict.grade().name, verdict.grade().temperature, facts, facts.chipHints(), slots,
            quotes, topics, lines.toActs(), slang.hints(), rewrite,
        )

    /** 작가 사용자 메시지 본문(JSON 한 줄). 테스트·실측 도구에서도 이 함수로 입력을 만든다. */
    fun inputText(objectMapper: ObjectMapper, rewrite: RoastRewrite? = null): String = objectMapper.writeValueAsString(input(rewrite))

    fun hasFact(key: String): Boolean = facts.find(key) != null

    /** [key] 칩 후보의 서버 칩 값. 없으면 null이다. */
    fun chipOf(key: String): RoastChip? = facts.find(key)?.chip

    /** [lineId]번 본인 줄의 말풍선 표시. 작가에게 보낸 본인 줄이 아니면 null이다. 상대 줄은 보내지 않으므로 늘 null이다. */
    fun quoteOf(lineId: Int): String? = lineQuotes.find(lineId)

    /** 슬롯 수만큼 칩 후보로 만든 통계 카드. 작가 근거가 검증을 끝내 통과하지 못했을 때 쓴다. */
    fun fallbackEvidence(fallback: RoastFallback): RoastEvidences = facts.chipFacts().fallbackEvidence(statSlots, fallback, ThanksFactSpec.KEY)

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

        /**
         * 메시지 [messages](본인과 상대, 시각순, 한국 시각)와 조서 집계 [stats], 선물 판정 [verdict], 항목 점수 [dimensions]로 입력을 조립한다.
         * 본인 줄만 쓰고, 사람 이름·사는 곳 이름·숫자·영문·링크는 가린다. 지명·가게·행사 이름은 그대로 둔다. [stats]가 없으면(조서 집계 실패) 사실·인용·화제 없이 보낸다.
         * 입버릇은 지금 사전으로 다시 읽은 집계를 쓴다. 테스트·실측 도구에서도 이 함수로 입력을 만든다.
         * 대화 참여자 이름 [participants](원본 파일 제출만 있음)는 모든 줄에서 가리고 차단 목록에 넣는다.
         * 근거 카드 슬롯 수는 항목 점수로 정하고, 말풍선 표시가 남는 본인 줄 수와 칩 후보 수로 줄인다([RoastSlots.limitedTo]).
         * [seed]는 사실 고르기 시드다([RoastFacts.of]). 같은 참가자·같은 해면 같은 값을 넘겨 재시도에도 같은 사실이 나오게 한다.
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
            return RoastBrief(
                verdict,
                facts,
                refreshed?.let { counted -> RoastQuotes.of(counted, catalog.rules) } ?: RoastQuotes.EMPTY,
                refreshed?.let { counted -> RoastTopics.of(counted, dossier.topics, catalog.rules.excludedTopics) } ?: RoastTopics.EMPTY,
                sent,
                all.blockedWords().plus(participants.toList()),
                catalog.rules.slang.allowed,
                slots.limitedTo(lineQuotes.usableCount(), facts.size()),
                lineQuotes,
                slots.limitedTo(Int.MAX_VALUE, facts.size()),
            )
        }
    }
}
