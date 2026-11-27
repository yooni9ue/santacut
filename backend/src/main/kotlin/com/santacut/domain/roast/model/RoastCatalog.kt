package com.santacut.domain.roast.model

/**
 * 팩폭 작가 리소스 묶음(resources/roast 아래 rules.json, slang.json, facts.json, fallback.json). slang.json은 [RoastRules.slang]으로 들어간다. 앱이 뜰 때 한 번 읽어 검사하고,
 * 위반이 있으면 앱을 띄우지 않는다.
 *
 * @property facts 조서 지표를 작가 사실로 옮기는 규칙
 * @property phrase 입버릇을 작가 사실로 옮기는 규칙
 * @property thanks 고맙다는 말 횟수를 작가 사실로 옮기는 규칙
 */
class RoastCatalog(
    val rules: RoastRules,
    val facts: RoastFactSpecs,
    val phrase: PhraseFactSpec,
    val thanks: ThanksFactSpec,
    val fallback: RoastFallback,
) {
    /** 모든 리소스의 규칙 위반 설명. 비어 있어야 앱을 띄운다. 칩 후보가 될 수 있는 모든 칩에 칩별 대체 문구가 있어야 한다. */
    fun violations(): List<String> =
        facts.violations() + phrase.violations() + thanks.violations() + fallback.violations(chipKeys()) + rules.slang.violations()

    /** 칩 후보가 될 수 있는 칩 key. 지표 칩, 고마워 칩, 입버릇 칩(머리말), 항목 점수 칩이다. */
    private fun chipKeys(): List<String> =
        facts.keys() + ThanksFactSpec.KEY + PhraseFactSpec.KEY_PREFIX + RoastEvidences.dimensionKeys()
}
