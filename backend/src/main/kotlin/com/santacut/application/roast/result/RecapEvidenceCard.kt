package com.santacut.application.roast.result

import com.santacut.domain.roast.model.EvidenceKind
import com.santacut.domain.roast.model.RoastEvidence
import com.santacut.domain.share.model.RecapDeckCard
import com.santacut.domain.share.model.RecapDeckKind

/**
 * 근거 카드(선물 받는 이유, 점수 깎인 말, 살아남은 말). 본인 말풍선과 서버 칩, 상 이름 라벨, 한 줄, 영역 태그를 싣는다.
 * 말풍선이 없으면([quote] null) 칩을 크게 찍는 통계 카드다.
 *
 * @property ordinal 같은 종류 안 순번(1부터). 키커("선물 받는 이유 1")에 쓴다.
 * @property quote 본인 말풍선(이름 가림, 20자 이하). 통계 카드면 null이다.
 * @property chipText 서버 칩 글자(예: `'고마워' 0번`)
 * @property label 상 이름 라벨(예: 답장독촉상)
 * @property line 한 줄(숫자 없음)
 * @property dimTag 영역 태그(예: "협력 +", "존중 −"). 칩 방향이 가리키는 항목이 있을 때만 있고, 없으면 null이다.
 */
data class RecapEvidenceCard(
    override val index: Int,
    override val kind: RecapCardKind,
    val ordinal: Int,
    val quote: String?,
    val chipText: String,
    val label: String,
    val line: String,
    val dimTag: String?,
) : RecapCard {
    override fun at(index: Int): RecapCard = copy(index = index)

    override fun toDeckCard(top: RecapTopResult, scoreText: String): RecapDeckCard = RecapDeckCard(
        index, RecapDeckKind.valueOf(kind.name), scoreText, top.rank, top.topPercent, ordinal = ordinal, quote = quote, chipText = chipText,
        label = label, line = line, dimTag = dimTag,
    )

    companion object {
        /** 저장한 근거 [evidence]를 같은 종류 안 [ordinal]번째 카드로 만든다. 영역 태그는 [dimTag]다. */
        fun of(evidence: RoastEvidence, ordinal: Int, dimTag: String?): RecapEvidenceCard = RecapEvidenceCard(
            0, kindOf(evidence.kind), ordinal, evidence.quote, evidence.chip?.text().orEmpty(), evidence.label, evidence.line, dimTag,
        )

        private fun kindOf(kind: EvidenceKind): RecapCardKind = when (kind) {
            EvidenceKind.REASON -> RecapCardKind.REASON
            EvidenceKind.CUT -> RecapCardKind.CUT
            EvidenceKind.KEEP -> RecapCardKind.KEEP
        }
    }
}
