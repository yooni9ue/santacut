package com.santacut.domain.roast.model

import com.santacut.domain.analysis.model.GiftGrade

/**
 * 작가 문장이 검증을 끝내 통과하지 못했을 때 쓰는 서버 대체 문장(roast/fallback.json).
 * 판정 한 줄과 결론 칭호·마지막 한 줄은 구간(등급)별 고정 문장이고, 근거 카드 라벨과 한 줄은 종류별이다.
 *
 * @property bands 등급 이름([GiftGrade.name])별 고정 문장
 * @property evidence 근거 카드 종류 key([EvidenceKind.key])별 대체 라벨과 한 줄
 */
data class RoastFallback(
    val bands: Map<String, RoastBandCopy>,
    val evidence: Map<String, EvidenceCopy>,
) {
    fun violations(): List<String> = listOfNotNull(
        "fallback.bands: 등급마다 문장이 있어야 한다".takeUnless { bands.keys.containsAll(GiftGrade.entries.map(GiftGrade::name)) },
        "fallback.evidence: 근거 종류마다 라벨과 한 줄이 있어야 한다".takeUnless {
            EvidenceKind.entries.all { kind -> evidence[kind.key]?.isMalformed() == false }
        },
    )

    fun bandOf(grade: GiftGrade): RoastBandCopy = bands.getValue(grade.name)

    /** [kind] 근거 카드 가운데 [index]번째(0부터) 통계 카드. 칩은 [fact]이고 말풍선은 없다. */
    fun evidenceOf(kind: EvidenceKind, index: Int, fact: RoastChipFact): RoastEvidence {
        val copy = evidence.getValue(kind.key)
        return RoastEvidence(kind, null, fact.key, fact.chip, null, copy.labelAt(index), copy.lineAt(index), RoastSource.FALLBACK)
    }

    /** [grade] 구간의 대체 결론. 대표 말풍선과 칩은 [hero] 근거 카드 것이고, 없으면 비운다. [plain]이면 유행어 없는 마지막 한 줄을 쓴다. */
    fun conclusionOf(grade: GiftGrade, hero: RoastEvidence?, plain: Boolean): RoastConclusion {
        val band = bandOf(grade)
        return RoastConclusion(
            RoastText(band.title, RoastSource.FALLBACK), hero?.lineId, hero?.chipKey, hero?.quote, hero?.chip,
            RoastText(band.finalLine(plain), RoastSource.FALLBACK),
        )
    }

    /** 모든 대체 문장. 배포 전에 검증 규칙을 통과하는지 테스트가 확인한다. */
    fun allBandTexts(): List<String> = bands.values.flatMap(RoastBandCopy::all)
}
