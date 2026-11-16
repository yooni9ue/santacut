package com.santacut.domain.analysis.model

import java.time.Duration
import java.time.LocalDateTime

/**
 * 채점 대화문의 한 줄. 같은 화자가 잇달아 보낸 메시지를 ` / `로 이은 것이다.
 *
 * @property speaker 화자 표시([ScoringMessage.speaker])
 * @property startedAt 첫 메시지 시각(한국 시각)
 * @property endedAt 마지막 메시지 시각(한국 시각)
 */
data class TranscriptTurn(
    val speaker: String,
    val startedAt: LocalDateTime,
    val endedAt: LocalDateTime,
    val text: String,
) {
    fun isSelf(): Boolean = speaker == ScoringMessage.SELF

    fun length(): Int = text.length

    /** 같은 화자가 [MERGE_GAP]보다 짧은 간격으로 이어 보낸 메시지면 이 줄에 잇는다. */
    fun canAbsorb(message: ScoringMessage): Boolean =
        message.speaker == speaker && Duration.between(endedAt, message.localTime) < MERGE_GAP

    fun absorbed(message: ScoringMessage): TranscriptTurn =
        copy(endedAt = message.localTime, text = text + MERGE_SEPARATOR + message.normalizedText())

    /** [previous] 줄이 끝난 뒤 이 줄이 시작하기까지의 간격. */
    fun gapAfter(previous: TranscriptTurn): Duration = Duration.between(previous.endedAt, startedAt)

    companion object {
        /** 이보다 오래 쉬고 이어 보낸 메시지는 같은 화자여도 새 줄로 둔다. 모델에 간격 표시를 보내는 문턱과 같다. */
        val MERGE_GAP: Duration = Duration.ofHours(3)

        private const val MERGE_SEPARATOR = " / "

        fun of(message: ScoringMessage): TranscriptTurn =
            TranscriptTurn(message.speaker, message.localTime, message.localTime, message.normalizedText())
    }
}
