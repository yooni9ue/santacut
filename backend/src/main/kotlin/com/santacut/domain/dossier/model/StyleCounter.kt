package com.santacut.domain.dossier.model

import com.santacut.domain.analysis.model.ScoringMessage
import java.time.Duration

/**
 * 본인 메시지 모양을 쌓는다. 길이와 비율은 글 메시지의 코드 포인트로, 연속 묶음은 모든 본인 메시지로 센다.
 * 상대 메시지가 끼거나 앞 본인 메시지와 [BURST_GAP]보다 벌어지면 새 묶음이다.
 */
class StyleCounter {
    private var textCount = 0
    private var totalLength = 0L
    private var shortCount = 0
    private var questionCount = 0
    private var exclaimCount = 0
    private var longest = 0
    private var burstCount = 0
    private var burstTotal = 0
    private var currentBurst = 0
    private var longestBurst = 0

    /** 앞 메시지 [previous] 뒤에 온 본인 메시지 [current]를 묶음에 넣는다. */
    fun addToBurst(previous: ScoringMessage?, current: ScoringMessage) {
        if (previous != null && continuesBurst(previous, current)) currentBurst++ else startBurst()
        burstTotal++
        longestBurst = maxOf(longestBurst, currentBurst)
    }

    /** 앞뒤 공백을 뗀 글 메시지 [text]의 모양을 더한다. */
    fun addText(text: String) {
        val length = text.codePointCount(0, text.length)
        textCount++
        totalLength += length
        if (length <= SHORT_LENGTH) shortCount++
        if (QUESTION in text) questionCount++
        if (EXCLAIM in text) exclaimCount++
        longest = maxOf(longest, length)
    }

    /** 자리표시와 빈 메시지를 뺀 글 메시지 수. 밀도와 비율의 분모다. */
    fun textCount(): Int = textCount

    fun stats(): MessageStyle = MessageStyle(
        DossierRatios.average(totalLength, textCount), DossierRatios.ratio(shortCount, textCount),
        DossierRatios.average(burstTotal.toLong(), burstCount), longestBurst,
        DossierRatios.ratio(questionCount, textCount), DossierRatios.ratio(exclaimCount, textCount), longest,
    )

    private fun startBurst() {
        burstCount++
        currentBurst = 1
    }

    private fun continuesBurst(previous: ScoringMessage, current: ScoringMessage): Boolean =
        previous.isSelf() && Duration.between(previous.localTime, current.localTime) <= BURST_GAP

    companion object {
        const val SHORT_LENGTH = 5
        private const val QUESTION = '?'
        private const val EXCLAIM = '!'
        private val BURST_GAP: Duration = Duration.ofMinutes(3)
    }
}
