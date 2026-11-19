package com.santacut.domain.dossier.model

import java.time.LocalDate

/**
 * 본인 메시지의 양을 쌓는다. 메시지 수와 활동일은 모든 본인 메시지로, 글자 수는 글 메시지의 코드 포인트(이모지 하나가 한 글자)로 센다.
 */
class VolumeCounter {
    private var messageCount = 0
    private var charCount = 0
    private val days = DayCounts()

    fun add(day: LocalDate) {
        messageCount++
        days.add(day)
    }

    /** 자리표시가 아닌 글 메시지 [text]의 글자 수를 더한다. */
    fun addText(text: String) {
        charCount += text.codePointCount(0, text.length)
    }

    fun messageCount(): Int = messageCount

    /** 쌓은 양. [textCount]는 글 메시지 수다. */
    fun stats(textCount: Int): VolumeStats =
        VolumeStats(messageCount, textCount, charCount, days.size(), days.busiest())
}
