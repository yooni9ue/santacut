package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.santacut.domain.analysis.model.GiftGrade
import com.santacut.domain.analysis.model.GiftVerdict

/**
 * 판정 한 줄과 결론 마지막 한 줄의 펀치 틀 풀(roast/punch.json). 마지막 한 줄은 구간(등급) 풀과 판정(GIFT, COAL) 풀을 합친 후보에서,
 * 판정 한 줄은 구간별 판정 풀([verdicts])에서 고른다. 어느 쪽이든 참가자·해 시드로 풀을 돌려 한 사람에게 틀 하나씩만 정해 준다.
 * 시드가 하나 차이인 이웃끼리도 다른 틀을 받아, 같은 구간 덱이 '올해 너는 OO야~'처럼 한 틀로 몰리지 않게 한다.
 * 구간 이름과 구간 밈은 그 구간만 쓴다([bandMarks]).
 *
 * @property bands 등급 이름([GiftGrade.name])별 마지막 한 줄 펀치 틀
 * @property skins 판정([GiftVerdict.skin])별 마지막 한 줄 펀치 틀
 * @property suggestions 작가에게 정해 주는 마지막 한 줄 틀 수
 * @property verdicts 등급 이름별 판정 한 줄 틀. 이 틀의 표지([PunchTemplate.marker])가 그 사람의 판정 한 줄 유행어 가족이다.
 * @property bandMarks 구간이 가진 표현. 다른 구간의 판정 한 줄·결론에 쓰지 못한다.
 */
data class RoastPunch(
    val bands: PunchPools,
    val skins: PunchPools,
    val suggestions: Int,
    val verdicts: PunchPools = PunchPools.EMPTY,
    @JsonProperty("band_marks") val bandMarks: BandMarks = BandMarks.EMPTY,
) {
    /**
     * 풀 위반. 등급·판정마다 풀이 있고, 마지막 한 줄 풀마다 표지가 [MIN_STRUCTURES]개 이상이며 유행어 없는 대체 문장이 하나 이상 있어야 한다.
     * 판정 한 줄 풀은 등급마다 표지가 [MIN_VERDICTS]개 이상이다. 어느 등급이 쓰는 틀도 다른 구간의 표현을 품지 않아야 한다.
     */
    fun violations(rules: RoastRules): List<String> = listOfNotNull(
        bands.missingGrades().takeIf(List<String>::isNotEmpty)?.let { names -> "punch.bands: 풀이 없는 등급 $names" },
        skins.missing(SKINS).takeIf(List<String>::isNotEmpty)?.let { names -> "punch.skins: 풀이 없는 판정 $names" },
        verdicts.missingGrades().takeIf(List<String>::isNotEmpty)?.let { names -> "punch.verdicts: 풀이 없는 등급 $names" },
        "punch.suggestions: ${MIN_SUGGESTIONS}~${MAX_SUGGESTIONS}개다".takeUnless { suggestions in MIN_SUGGESTIONS..MAX_SUGGESTIONS },
    ) + bands.violations(BANDS_FIELD, MIN_STRUCTURES, rules) + skins.violations(SKINS_FIELD, MIN_STRUCTURES, rules) +
        verdicts.templateViolations(VERDICTS_FIELD, MIN_VERDICTS, rules) + bandMarks.violations() + foreignViolations()

    /** 판정 [verdict] 구간이 마지막 한 줄에 쓸 수 있는 모든 틀(구간 풀과 판정 풀). 배포 전에 대체 문장이 검증 규칙을 통과하는지 테스트가 확인한다. */
    fun poolOf(verdict: GiftVerdict): PunchTemplates = bands.of(verdict.grade().name) + skins.of(verdict.skin())

    /**
     * 판정 [verdict] 구간에서 시드 [seed]가 받는 판정 한 줄 틀. 판정 풀을 시드로 돌린 순서에서 정해 준 마지막 한 줄 틀([candidatesFor]의 맨 앞)과
     * 펀치가 겹치지 않는 첫 틀이라, 판정 한 줄과 마지막 한 줄이 서로 다른 유행어 가족을 쓴다. 풀이 비었으면 null이다.
     */
    fun verdictFor(verdict: GiftVerdict, seed: Int, rules: RoastRules): PunchTemplate? {
        val finalPunch = candidatesFor(verdict, seed).at(0)
        val rotated = verdicts.of(verdict.grade().name).rotatedBy(seed)
        return rotated.without(finalPunch, rules).at(0) ?: rotated.at(0)
    }

    /**
     * 판정 [verdict] 구간의 마지막 한 줄 후보를 시드 [seed]로 돌린 순서. 표지가 겹치는 틀을 뺀 풀을 시드 자리부터 돌리므로 시드가 하나 다른 이웃은 맨 앞 틀이 다르다.
     * 앞 [suggestions]개가 작가에게 정해 주는 틀이고, 나머지는 대체 문장이 판정 한 줄과 겹칠 때 넘겨 볼 예비다.
     */
    fun candidatesFor(verdict: GiftVerdict, seed: Int): PunchTemplates = poolOf(verdict).distinctMarkers().rotatedBy(seed)

    /** [grade] 구간이 쓸 수 없는 다른 구간의 표현. */
    fun foreignMarksFor(grade: GiftGrade): ForeignMarks = bandMarks.foreignTo(grade)

    private fun foreignViolations(): List<String> = GiftGrade.entries.flatMap { grade ->
        val foreign = foreignMarksFor(grade)
        val texts = (poolOf(GiftVerdict(gradeSample(grade))) + verdicts.of(grade.name)).toList().flatMap { punch -> listOf(punch.template, punch.fallback) }
        texts.mapNotNull { text -> foreign.foundIn(text)?.let { mark -> "punch: ${grade.name} 틀이 다른 구간 표현($mark)을 쓴다: $text" } }
    }

    companion object {
        /** 마지막 한 줄 풀 하나에 있어야 하는 서로 다른 펀치 구조(표지) 수. */
        const val MIN_STRUCTURES = 6

        /** 판정 한 줄 풀 하나에 있어야 하는 서로 다른 표지 수. */
        const val MIN_VERDICTS = 3
        private const val MIN_SUGGESTIONS = 1
        private const val MAX_SUGGESTIONS = 3
        private const val BANDS_FIELD = "punch.bands"
        private const val SKINS_FIELD = "punch.skins"
        private const val VERDICTS_FIELD = "punch.verdicts"
        private val SKINS = listOf(GiftVerdict.GIFT_KEY.uppercase(), GiftVerdict.COAL_KEY.uppercase())

        /** 펀치 틀이 없는 풀. 테스트·옛 설정에서 쓴다. */
        val EMPTY = RoastPunch(PunchPools.EMPTY, PunchPools.EMPTY, MIN_SUGGESTIONS)

        /** [grade] 구간에 드는 총점(100분의 1점) 하나. 등급의 판정(GIFT, COAL) 풀을 고를 때 쓴다. */
        private fun gradeSample(grade: GiftGrade): Int = (0..MAX_HUNDREDTHS step GRADE_STEP).first { hundredths -> GiftGrade.of(hundredths) == grade }
        private const val MAX_HUNDREDTHS = 10_000
        private const val GRADE_STEP = 100
    }
}
