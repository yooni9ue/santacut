package com.santacut.domain.analysis.model

import java.time.Duration
import java.time.LocalDateTime
import java.time.YearMonth

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

    /**
     * 같은 화자가 [MERGE_GAP]보다 짧은 간격으로 이어 보낸 메시지면 이 줄에 잇는다. 이으면 [MAX_MERGED_LENGTH]자를 넘을 때는
     * 새 줄로 둔다. 혼잣말 수백 개가 한 줄이 되면 대화문 길이 상한에 걸려 그 줄이 통째로 빠지기 때문이다.
     */
    fun canAbsorb(message: ScoringMessage): Boolean =
        message.speaker == speaker && Duration.between(endedAt, message.localTime) < MERGE_GAP &&
            text.length + MERGE_SEPARATOR.length + message.normalizedText().length <= MAX_MERGED_LENGTH

    fun absorbed(message: ScoringMessage): TranscriptTurn =
        copy(endedAt = message.localTime, text = text + MERGE_SEPARATOR + message.normalizedText())

    /** 잇달아 보낸 메시지를 이은 본문을 다시 메시지별 토막으로 나눈다. */
    fun pieces(): List<String> = text.split(MERGE_SEPARATOR)

    /** 달마다 고르게 고를 때 쓰는 후보. 첫 메시지가 속한 달이고, 몫은 본문 글자 수다. */
    fun toSpreadCandidate(): SpreadCandidate<TranscriptTurn> = SpreadCandidate(this, YearMonth.from(startedAt), length())

    /** [previous] 줄이 끝난 뒤 이 줄이 시작하기까지의 간격. */
    fun gapAfter(previous: TranscriptTurn): Duration = Duration.between(previous.endedAt, startedAt)

    companion object {
        /** 이보다 오래 쉬고 이어 보낸 메시지는 같은 화자여도 새 줄로 둔다. 모델에 간격 표시를 보내는 문턱과 같다. */
        val MERGE_GAP: Duration = Duration.ofHours(3)

        /** 이어 붙인 한 줄 본문의 최대 글자 수. 대화문 길이 상한([ScoringDigest.MAX_TRANSCRIPT_CHARS])의 작은 일부다. */
        const val MAX_MERGED_LENGTH = 2000

        private const val MERGE_SEPARATOR = " / "

        fun of(message: ScoringMessage): TranscriptTurn =
            TranscriptTurn(message.speaker, message.localTime, message.localTime, message.normalizedText())
    }
}
