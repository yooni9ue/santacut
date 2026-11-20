package com.santacut.domain.roast.model

import com.santacut.domain.analysis.model.ScoringMessage
import com.santacut.domain.dossier.model.DossierAct

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

        /**
         * 메시지 [messages] 가운데 본인 글 메시지만 순번을 매겨 가린다. 통째로 뺄 줄은 순번만 쓰고 넣지 않는다.
         * 두 번 가린다. 처음에 호칭·부르는 말에서 이름 줄기를 모으고, 그 줄기가 다른 줄에 호칭 없이 나와도 가린다.
         */
        fun of(messages: List<ScoringMessage>, rules: RoastRules): RoastLines {
            val first = messages.filter { message -> message.isSelf() && !message.isPlaceholder() }
                .mapIndexedNotNull { index, message -> RoastLine.of(index + 1, message, rules) }
            val pattern = rules.namePattern(first.flatMap(RoastLine::names)) ?: return RoastLines(first)
            return RoastLines(first.map { line -> line.remaskedBy(pattern) })
        }
    }
}
