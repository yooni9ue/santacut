package com.santacut.application.roast.result

import com.santacut.domain.analysis.model.GiftVerdict
import com.santacut.domain.analysis.model.ScoreDimension
import com.santacut.domain.ranking.model.ResultProfile
import com.santacut.domain.roast.model.EvidenceKind
import com.santacut.domain.roast.model.RoastContent
import com.santacut.domain.roast.model.RoastEvidence
import com.santacut.domain.roast.model.RoastEvidences
import com.santacut.domain.share.model.RecapDeckCards
import java.time.Instant

/**
 * 리캡 카드 덱의 순서. 카드 종류 순서를 정하는 유일한 곳이다.
 * 선물 덱은 판정, 컷 표, 선물 받는 이유(최대 셋), 점수 깎인 말(최대 둘), 결론이고, 석탄 덱은 판정, 컷 표, 점수 깎인 말(최대 셋), 살아남은 말 하나, 결론이다.
 * 덱은 다섯 장에서 여덟 장이다. 근거 카드 수와 최소 장수는 도메인([RoastContent.forDeck])이 맞춰 넘긴다.
 * 로그인 사용자 리캡 응답과 공유 링크 카드 스냅숏([toDeckCards])이 모두 이 목록을 따른다.
 */
data class RecapCards(val values: List<RecapCard>) {
    /** 공유 링크에 저장할 카드 목록. 순서와 index는 그대로이고 모든 카드에 상단 바 [top]을 싣는다. */
    fun toDeckCards(top: RecapTopResult, scoreText: String): RecapDeckCards =
        RecapDeckCards(values.map { card -> card.toDeckCard(top, scoreText) })

    companion object {
        private const val PLUS = " +"
        private const val MINUS = " −"

        /**
         * 판정 [gift], 덱에 맞춘 팩폭 내용 [content], 꾸밈 값 [profile], 선물 도착 예정 시각 [eta]로 덱을 만든다.
         * 근거 카드의 영역 태그는 그 카드의 칩이 항목 점수 칩(dimension_으로 시작)일 때만 단다. 선물 받는 이유면 "+", 점수 깎인 말이면 "−"이고,
         * 살아남은 말이나 작가 카드·입버릇 칩 카드처럼 어느 항목의 근거인지 서버가 모르는 카드는 null이다. 카드 순서로 항목을 짐작하지 않는다.
         */
        fun of(gift: GiftVerdict, content: RoastContent, profile: ResultProfile, eta: Instant?): RecapCards {
            val evidence = content.evidence.toList()
            val cards = listOf(RecapVerdictCard.of(gift, content.verdictLine.text, eta), RecapCutlineCard.of(gift.grade())) +
                evidence.mapIndexed { position, item -> evidenceCard(item, evidence.take(position)) } +
                listOfNotNull(content.conclusion?.let { conclusion -> RecapConclusionCard.of(gift, profile, conclusion) })
            return RecapCards(cards.mapIndexed { index, card -> card.at(index) })
        }

        private fun evidenceCard(evidence: RoastEvidence, before: List<RoastEvidence>): RecapEvidenceCard {
            val ordinal = before.count { previous -> previous.kind == evidence.kind }
            return RecapEvidenceCard.of(evidence, ordinal + 1, dimTagOf(evidence))
        }

        private fun dimTagOf(evidence: RoastEvidence): String? {
            val sign = when (evidence.kind) {
                EvidenceKind.REASON -> PLUS
                EvidenceKind.CUT -> MINUS
                EvidenceKind.KEEP -> return null
            }
            val dimension = ScoreDimension.ofKey(evidence.chipKey.removePrefix(RoastEvidences.DIMENSION_KEY_PREFIX).takeIf {
                evidence.chipKey.startsWith(RoastEvidences.DIMENSION_KEY_PREFIX)
            })
            return dimension?.let { found -> found.label + sign }
        }
    }
}
