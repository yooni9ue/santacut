package com.santacut.domain.analysis.model

import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException

/**
 * AI 채점에 쓰는 입력. 분석 기간의 대화 전체(본인과 상대)를 정규화한 대화문([turns])이다.
 * 너무 길면 달마다 고르게 줄을 남기고([TranscriptTurns.spreadWithin]) 뺀 줄 수를 [omittedTurns]에 적는다.
 *
 * @property messageCount 분석 기간의 메시지 수. AI에는 보내지 않고 사용량 로그에만 남긴다.
 * @property selfMessageCount 대화문에 들어간 본인 메시지 수. 잇달아 보낸 메시지를 한 줄로 잇기 전의 수다.
 */
class ScoringDigest(
    val messageCount: Int,
    val selfMessageCount: Int,
    val turns: TranscriptTurns,
    val omittedTurns: Int,
) {
    /** AI가 돌려줄 수 있는 근거 번호의 상한. */
    fun lineCount(): Int = turns.size()

    /**
     * 본인 메시지가 [MIN_SELF_MESSAGES]개보다 적거나 본인 본문이 [MIN_SELF_CHARS]자보다 짧으면 AI를 부르지 않고 근거 부족으로 끝낸다.
     * 이만큼도 없으면 점수를 매길 내용이 거의 없어 호출 비용만 든다. 문턱은 일부러 낮게 잡았다.
     */
    fun verifyEnough(): ScoringDigest {
        if (selfMessageCount < MIN_SELF_MESSAGES || turns.selfLength() < MIN_SELF_CHARS) {
            throw SantaCutException(ErrorCode.INSUFFICIENT_EVIDENCE)
        }
        return this
    }

    companion object {
        const val MIN_SELF_MESSAGES = 8
        const val MIN_SELF_CHARS = 300

        /**
         * 대화문 본문 글자 수 합의 상한. 한국어 대화문은 약 1.4자가 토큰 하나라, 줄 번호·화자 표시와 구간마다 붙는 시스템 프롬프트를
         * 더해도 입력이 약 6만 토큰을 넘지 않게 잡았다. 상대 본문을 짧게 잘라([ScoringMessage.normalizedText]) 본인 말이 더 많이 들어간다.
         */
        const val MAX_TRANSCRIPT_CHARS = 52_000

        fun of(messages: ScoringMessages): ScoringDigest {
            val all = messages.toTurns()
            val kept = all.spreadWithin(MAX_TRANSCRIPT_CHARS)
            return ScoringDigest(messages.size(), messages.selfCount(), kept, all.size() - kept.size())
        }
    }
}
