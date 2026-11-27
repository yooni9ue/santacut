package com.santacut.domain.roast.model

import com.santacut.domain.analysis.model.GiftGrade

/**
 * 작가 문장이 검증을 끝내 통과하지 못했을 때 쓰는 서버 대체 문장(roast/fallback.json).
 * 판정 한 줄과 결론 칭호·마지막 한 줄은 구간(등급)별 고정 문장이다. 근거 카드 라벨과 한 줄은 칩마다·근거 종류마다 따로 두어([chips])
 * 통계 카드가 그 칩을 말하게 하고, 칩별 문구가 없는 칩(옛 결과의 칩 등)만 종류별 기본 문구([evidence])를 쓴다.
 *
 * @property bands 등급 이름([GiftGrade.name])별 고정 문장
 * @property evidence 근거 카드 종류 key([EvidenceKind.key])별 기본 대체 라벨과 한 줄
 * @property chips 칩 key(또는 머리말)별·근거 종류별 대체 라벨과 한 줄
 */
data class RoastFallback(
    val bands: Map<String, RoastBandCopy>,
    val evidence: Map<String, EvidenceCopy>,
    val chips: ChipCopies = ChipCopies.EMPTY,
) {
    /** 문구 위반. 칩 후보가 될 수 있는 칩 key [chipKeys]마다 칩별 문구가 있어야 한다. */
    fun violations(chipKeys: List<String> = emptyList()): List<String> = listOfNotNull(
        "fallback.bands: 등급마다 문장이 있어야 한다".takeUnless { bands.keys.containsAll(GiftGrade.entries.map(GiftGrade::name)) },
        "fallback.evidence: 근거 종류마다 라벨과 한 줄이 있어야 한다".takeUnless {
            EvidenceKind.entries.all { kind -> evidence[kind.key]?.isMalformed() == false }
        },
        "fallback.chips: 칩별 문구마다 key나 머리말, 종류별 고정 라벨·한 줄이 있어야 한다".takeIf { chips.hasMalformed() },
        chips.missing(chipKeys).takeIf(List<String>::isNotEmpty)?.let { keys -> "fallback.chips: 문구가 없는 칩 $keys" },
    )

    fun bandOf(grade: GiftGrade): RoastBandCopy = bands.getValue(grade.name)

    /**
     * [kind] 근거 카드 가운데 [index]번째(0부터) 통계 카드. 칩은 [fact]이고 말풍선은 없다.
     * 문구는 그 칩의 칩별 문구를 쓰고, 입버릇 칩이면 칩 이름의 입버릇 표기를 틀에 넣는다. 칩별 문구가 없으면 종류별 기본 문구다.
     * [mild]면 깎인 말에 순한 문구([ChipCopy.mildCut])를 쓴다. 높은 점수 가운데 가장 낮아 억지로 깎인 말이 된 칩이 그렇다.
     */
    fun evidenceOf(kind: EvidenceKind, index: Int, fact: RoastChipFact, mild: Boolean = false): RoastEvidence {
        val copy = chips.find(fact.key)?.copyOf(kind, mild) ?: evidence.getValue(kind.key)
        val phrase = fact.chip.quotedPhrase()
        return RoastEvidence(kind, null, fact.key, fact.chip, null, copy.labelAt(index, phrase), copy.lineAt(index, phrase), RoastSource.FALLBACK)
    }

    /**
     * [grade] 구간의 대체 결론. 대표 말풍선과 칩은 [hero] 근거 카드 것이고, 없으면 비운다. 고마워 칩은 대표 칩으로 쓰지 않는다.
     * [plain]이면 유행어 없는 마지막 한 줄을 쓴다.
     */
    fun conclusionOf(grade: GiftGrade, hero: RoastEvidence?, plain: Boolean): RoastConclusion =
        conclusionOf(grade, hero, bandOf(grade).finalLine(plain))

    /** [grade] 구간의 대체 결론에 마지막 한 줄 [finalLine](이 사람의 펀치 틀 대체 문장)을 붙인다. 칭호와 대표 말·칩은 위와 같다. */
    fun conclusionOf(grade: GiftGrade, hero: RoastEvidence?, finalLine: String): RoastConclusion {
        val chip = hero?.takeUnless { evidence -> evidence.chipKey == ThanksFactSpec.KEY }
        return RoastConclusion(
            RoastText(bandOf(grade).title, RoastSource.FALLBACK), hero?.lineId, chip?.chipKey, hero?.quote, chip?.chip,
            RoastText(finalLine, RoastSource.FALLBACK),
        )
    }

    /** 모든 대체 문장. 배포 전에 검증 규칙을 통과하는지 테스트가 확인한다. */
    fun allBandTexts(): List<String> = bands.values.flatMap(RoastBandCopy::all)
}
