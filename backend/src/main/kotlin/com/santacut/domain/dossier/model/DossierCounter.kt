package com.santacut.domain.dossier.model

import com.santacut.domain.analysis.model.DataSource
import com.santacut.domain.analysis.model.ScoringMessage

/**
 * 시각순 메시지를 하나씩 받아 조서 집계값을 쌓는다([DossierMessages]가 한 번 훑으며 넘긴다).
 * 본인 메시지는 모두 양·시각·연속 묶음에 세고, 사진·이모티콘 같은 자리표시와 빈 메시지를 뺀 글 메시지만 글자·사전·모양에 센다.
 * 밀도와 비율의 분모도 글 메시지 수다. 상대 메시지는 답장 간격과 묶음 경계에만 쓴다.
 */
class DossierCounter(private val catalog: DossierCatalog) {
    private val volume = VolumeCounter()
    private val clock = ClockCounter()
    private val laugh = LaughCounter()
    private val style = StyleCounter()
    private val words = WordCounter(catalog)
    private val reply = ReplyCounter()
    private var previous: ScoringMessage? = null

    fun add(message: ScoringMessage) {
        reply.add(previous, message)
        if (message.isSelf()) addSelf(message)
        previous = message
    }

    /** 쌓은 값으로 집계값과 톤을 낸다. */
    fun tally(source: DataSource): DossierTally {
        val textCount = style.textCount()
        val stats = DossierStats(
            DossierStats.VERSION, volume.stats(textCount), laugh.laughStats(textCount), laugh.emojiStats(textCount),
            clock.stats(volume.messageCount()), reply.stats(source), style.stats(), words.stats(textCount),
        )
        return DossierTally(stats, words.tone(textCount))
    }

    private fun addSelf(message: ScoringMessage) {
        val day = DossierCalendar.dayOf(message)
        volume.add(day)
        clock.add(message, day)
        style.addToBurst(previous, message)
        if (message.isPlaceholder() || message.text.isBlank()) return
        volume.addText(message.text)
        laugh.add(message)
        style.addText(message.text.trim())
        words.add(message.normalizedFullText(), DossierAct.of(day.monthValue))
    }
}
