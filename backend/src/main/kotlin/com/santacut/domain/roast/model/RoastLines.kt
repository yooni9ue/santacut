package com.santacut.domain.roast.model

import com.santacut.domain.analysis.model.ScoringMessage
import com.santacut.domain.dossier.model.DossierAct
import java.time.Duration

/**
 * 작가에게 보낼 본인 말풍선 줄 목록. 상대 말풍선은 넣지 않는다.
 */
class RoastLines(private val values: List<RoastLine>) {
    fun size(): Int = values.size

    /**
     * 막마다 [charsPerAct]자 안으로 줄인 목록. 막 안에서 최근 줄부터 담고 같은 본문은 한 번만 담는다(되풀이한 버릇은 사실 목록이 센다).
     * 줄 순서는 원래 순번이다.
     */
    fun trimmed(charsPerAct: Int): RoastLines = RoastLines(DossierAct.entries.flatMap { act -> recentWithin(act, charsPerAct) })

    /** 가린 원래 낱말과 이름 줄기 전체(차단 목록). */
    fun blockedWords(): BlockedWords = BlockedWords(values.flatMap { line -> line.masked + line.names })

    /** 말풍선 표시에서 ○○로 가릴 낱말 전체([RoastLine.quoteWords]). */
    fun quoteWords(): List<String> = values.flatMap(RoastLine::quoteWords)

    /** 이름 패턴 [names]로 가린 줄마다의 말풍선 표시. */
    fun quotes(names: MaskPattern?): LineQuotes = LineQuotes(values.map { line -> line.quoteBy(names) })

    /** 어느 줄 본문에 [text]가 글자 그대로 있는지. */
    fun containsText(text: String): Boolean = values.any { line -> text in line.text }

    /** [text]에서 [length]자 이상 이어져 어느 줄 본문에 그대로 있는 첫 구간([length]자). 공백만 있는 구간은 보지 않는다. 없으면 null이다. */
    fun sharedRun(text: String, length: Int): String? {
        if (text.length < length) return null
        val joined = values.joinToString(LINE_BREAK, transform = RoastLine::text)
        return text.windowed(length).firstOrNull { window -> window.isNotBlank() && window in joined }
    }

    /** 막마다 나눈 작가 입력. 줄이 없는 막도 빈 목록으로 넣는다. */
    fun toActs(): List<RoastTranscriptAct> = DossierAct.entries.mapIndexed { index, act ->
        RoastTranscriptAct(index + 1, act.monthsText(), values.filter { line -> line.act() == act }.map(RoastLine::toInputLine))
    }

    private fun recentWithin(act: DossierAct, budget: Int): List<RoastLine> {
        val recent = values.filter { line -> line.act() == act }.sortedByDescending(RoastLine::number).distinctBy(RoastLine::text)
        val totals = recent.runningFold(0) { total, line -> total + line.inputLength() }.drop(1)
        return recent.filterIndexed { index, _ -> totals[index] <= budget }.sortedBy(RoastLine::number)
    }

    companion object {
        val EMPTY = RoastLines(emptyList())
        private const val LINE_BREAK = "\n"

        /** 본인 말풍선끼리 이 간격 안이면 연달아 보낸 묶음이다. 조서 집계(연달아 보내기)와 같은 기준이다. */
        private val BURST_GAP: Duration = Duration.ofMinutes(3)

        /**
         * 메시지 [messages] 가운데 본인 글 메시지만 순번을 매겨 가린다. 빈 본문은 순번만 쓰고 넣지 않는다.
         * 앞이나 뒤 메시지가 [BURST_GAP] 안에 보낸 본인 메시지면(조서의 연달아 보내기와 같은 기준) 그 줄은 연달아 보낸 묶음 안이다.
         * 두 번 가린다. 처음에 호칭·부르는 말에서 이름 줄기를 모으고, 그 줄기와 참여자 이름 [participants]가 어느 줄에 나와도 가린다.
         */
        fun of(messages: List<ScoringMessage>, rules: RoastRules, participants: ParticipantNames = ParticipantNames.EMPTY): RoastLines {
            val first = messages.indices.filter { index -> messages[index].isSelf() && !messages[index].isPlaceholder() }
                .mapIndexedNotNull { number, index -> RoastLine.of(number + 1, messages[index], rules, inBurst(messages, index)) }
            val pattern = rules.namePattern(first.flatMap(RoastLine::names) + participants.toList()) ?: return RoastLines(first)
            return RoastLines(first.map { line -> line.remaskedBy(pattern) })
        }

        private fun inBurst(messages: List<ScoringMessage>, index: Int): Boolean =
            (index > 0 && continues(messages[index - 1], messages[index])) ||
                (index < messages.lastIndex && continues(messages[index], messages[index + 1]))

        private fun continues(previous: ScoringMessage, next: ScoringMessage): Boolean =
            previous.isSelf() && next.isSelf() && Duration.between(previous.localTime, next.localTime) <= BURST_GAP
    }
}
