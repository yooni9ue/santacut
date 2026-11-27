package com.santacut.presentation.roast.response

import com.fasterxml.jackson.annotation.JsonValue
import com.santacut.application.roast.result.RecapCard
import com.santacut.application.roast.result.RecapCardKind
import com.santacut.application.roast.result.RecapConclusionCard
import com.santacut.application.roast.result.RecapCutlineCard
import com.santacut.application.roast.result.RecapEvidenceCard
import com.santacut.application.roast.result.RecapResult
import com.santacut.application.roast.result.RecapVerdictCard

/**
 * 리캡 카드 덱. 순서는 [RecapResult.cards]([com.santacut.application.roast.result.RecapCards])를 그대로 따른다.
 * 선물 덱은 판정, 컷 표, 선물 받는 이유, 점수 깎인 말, 결론이고, 석탄 덱은 판정, 컷 표, 점수 깎인 말, 살아남은 말, 결론이다.
 * JSON에서는 카드 배열 하나로 나간다.
 */
class RecapCardResponses(@field:JsonValue private val values: List<RecapCardResponse>) {
    companion object {
        fun from(result: RecapResult): RecapCardResponses = RecapCardResponses(result.cards.values.map { card -> responseOf(card, result) })

        private fun responseOf(card: RecapCard, result: RecapResult): RecapCardResponse = when (card) {
            is RecapVerdictCard -> VerdictCardResponse.from(card, result.top, result.scoreText)
            is RecapCutlineCard -> CutlineCardResponse.from(card, result.top, result.scoreText)
            is RecapEvidenceCard -> evidenceOf(card, result)
            is RecapConclusionCard -> ConclusionCardResponse.from(card, result.top, result.scoreText)
        }

        private fun evidenceOf(card: RecapEvidenceCard, result: RecapResult): RecapCardResponse = when (card.kind) {
            RecapCardKind.CUT -> CutCardResponse.from(card, result.top, result.scoreText)
            RecapCardKind.KEEP -> KeepCardResponse.from(card, result.top, result.scoreText)
            else -> ReasonCardResponse.from(card, result.top, result.scoreText)
        }
    }
}
