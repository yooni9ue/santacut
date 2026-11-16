package com.santacut.domain.analysis.model

import java.time.LocalDate
import java.util.Collections
import java.util.IdentityHashMap

/**
 * 표본으로 뽑을 수 있는 본인 메시지. 무작위 없이 같은 입력에서는 늘 같은 표본을 고른다.
 *
 * 달력 주(월요일 시작)로 나누고, 주마다 메시지를 칸으로 가른다. 항목([ScoreTheme])마다 그 낱말이 처음 걸린 메시지 칸이 하나씩,
 * 어느 낱말도 없는 메시지 칸이 하나 있다. 칸 안에서는 시각순으로 놓고 가운데, 사분점, 팔분점 순서로 뽑아
 * 몇 개를 뽑든 그 주 안에서 고르게 퍼지게 한다. 그리고 모든 주의 모든 칸에서 첫째, 다시 모든 주의 모든 칸에서 둘째 순서(라운드 로빈)로 뽑는다.
 * [MAX_SAMPLE_MESSAGES]개나 본문 [MAX_SAMPLE_CHARS]자를 넘기기 전에 멈추고, 뽑은 메시지는 시각순으로 놓는다. 같은 시각이면 받은 순서를 지킨다.
 */
class SampleCandidates private constructor(private val values: List<ScoringMessage>) {
    fun pick(): SampleMessages {
        val ordered = inRoundRobinOrder()
        val withinChars = ordered.runningFold(0) { total, message -> total + message.length() }
            .drop(1)
            .count { total -> total <= MAX_SAMPLE_CHARS }
        val picked = Collections.newSetFromMap(IdentityHashMap<ScoringMessage, Boolean>())
        picked += ordered.take(minOf(withinChars, MAX_SAMPLE_MESSAGES))
        return SampleMessages(values.filter { message -> message in picked }.sortedBy { message -> message.localTime })
    }

    private fun inRoundRobinOrder(): List<ScoringMessage> =
        values.groupBy { message -> message.weekStart() }
            .flatMap { (week, messages) -> slotsOf(week, messages) }
            .sortedWith(SampleSlot.ORDER)
            .map { slot -> slot.message }

    private fun slotsOf(week: LocalDate, messages: List<ScoringMessage>): List<SampleSlot> =
        messages.groupBy { message -> bucketOf(message) }.flatMap { (bucket, bucketMessages) ->
            val byTime = bucketMessages.sortedBy { message -> message.localTime }
            spreadOrder(byTime.size).mapIndexed { round, index -> SampleSlot(round, week, bucket, byTime[index]) }
        }

    /** 처음 걸린 항목의 순번. 어느 낱말도 없으면 마지막 칸([ScoreTheme] 수)이다. */
    private fun bucketOf(message: ScoringMessage): Int =
        ScoreTheme.entries.indexOfFirst { theme -> message.mentions(theme) }.takeIf { it >= 0 } ?: ScoreTheme.entries.size

    /** 0부터 [size]-1까지의 번호를 구간 가운데부터 차례로 반씩 나눈 순서로 놓는다. 앞에서 몇 개를 잘라도 고르게 퍼진다. */
    private fun spreadOrder(size: Int): List<Int> {
        val order = mutableListOf<Int>()
        val ranges = ArrayDeque(listOf(0 until size))
        while (ranges.isNotEmpty()) {
            val range = ranges.removeFirst()
            if (range.isEmpty()) continue
            val middle = (range.first + range.last) / 2
            order += middle
            ranges += range.first until middle
            ranges += middle + 1..range.last
        }
        return order
    }

    companion object {
        /** 표본 메시지 수의 상한. */
        const val MAX_SAMPLE_MESSAGES = 120

        /** 표본 본문 글자 수 합의 상한. */
        const val MAX_SAMPLE_CHARS = 6000

        /** 표본에 넣는 메시지 하나의 최대 글자 수. 더 긴 메시지는 자른다. */
        const val MAX_SAMPLE_TEXT_LENGTH = 200

        /** 아주 짧거나 웃음·이모지뿐인 메시지를 빼고, 긴 메시지는 [MAX_SAMPLE_TEXT_LENGTH]자로 자른다. */
        fun from(messages: List<ScoringMessage>): SampleCandidates = SampleCandidates(
            messages.filterNot { message -> message.isLowSignal() }.map { message -> message.truncated(MAX_SAMPLE_TEXT_LENGTH) },
        )
    }
}
