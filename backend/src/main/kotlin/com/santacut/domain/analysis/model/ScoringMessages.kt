package com.santacut.domain.analysis.model

/**
 * 채점에 쓰는 분석 기간의 메시지 전체(본인과 상대). 시각순이다. 여기서 AI에 보낼 대화문을 만든다.
 */
class ScoringMessages(private val values: List<ScoringMessage>) {
    fun size(): Int = values.size

    /** 메시지 목록의 방어 복사본. 조서 집계가 같은 메시지를 다시 센다. */
    fun toList(): List<ScoringMessage> = values.toList()

    /** 대화문에 들어가는(자리표시도 빈 본문도 아닌) 본인 메시지 수. */
    fun selfCount(): Int = values.count { message -> message.isSelf() && !message.isPlaceholder() && message.normalizedText().isNotEmpty() }

    /**
     * 첨부·삭제 안내와 빈 메시지를 빼고, 같은 화자가 잇달아 보낸 메시지를 한 줄로 이은 대화문.
     * 줄 규칙은 [TranscriptTurn]이 정한다.
     */
    fun toTurns(): TranscriptTurns {
        val turns = mutableListOf<TranscriptTurn>()
        values.filterNot { message -> message.isPlaceholder() || message.normalizedText().isEmpty() }.forEach { message ->
            val last = turns.lastOrNull()
            if (last != null && last.canAbsorb(message)) turns[turns.lastIndex] = last.absorbed(message) else turns += TranscriptTurn.of(message)
        }
        return TranscriptTurns(turns)
    }
}
