package com.santacut.domain.dossier.model

import com.santacut.domain.analysis.model.ScoringMessage

/**
 * 본인 글 메시지의 웃음(ㅋ·ㅎ)·울음(ㅠ·ㅜ) 글자와 이모지를 원래 본문으로 쌓는다. 밀도의 분모는 글 메시지 수다.
 */
class LaughCounter {
    private var laughs = 0
    private var cries = 0
    private var longestRun: LaughRun? = null
    private var emojiTotal = 0
    private val emojis = EmojiCounts()

    fun add(message: ScoringMessage) {
        laughs += charactersOf(message.text, LAUGH_CHARACTER)
        cries += charactersOf(message.text, CRY_CHARACTER)
        keepLongerRun(message)
        val found = Graphemes.of(message.text).emojis()
        emojiTotal += emojis.addAll(found)
    }

    fun laughStats(textCount: Int): LaughStats =
        LaughStats(laughs, DossierRatios.per100(laughs, textCount), longestRun, cries, DossierRatios.per100(cries, textCount))

    fun emojiStats(textCount: Int): EmojiStats = EmojiStats(emojis.top(EmojiTallies.LIMIT), DossierRatios.per100(emojiTotal, textCount))

    /** 이 메시지의 가장 긴 ㅋ 연타가 지금까지보다 길면 그 길이와 달·시를 남긴다. 같으면 먼저 나온 연타다. */
    private fun keepLongerRun(message: ScoringMessage) {
        val length = LAUGH_RUN.findAll(message.text).maxOfOrNull { run -> run.value.length } ?: return
        if (length > (longestRun?.length ?: 0)) longestRun = LaughRun(length, message.localTime.monthValue, message.localTime.hour)
    }

    private fun charactersOf(text: String, pattern: Regex): Int = text.length - text.replace(pattern, "").length

    companion object {
        private val LAUGH_CHARACTER = Regex("[ㅋㅎ]")
        private val CRY_CHARACTER = Regex("[ㅠㅜ]")
        private val LAUGH_RUN = Regex("ㅋ+")
    }
}
