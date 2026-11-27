package com.santacut.domain.roast.model

import com.santacut.domain.analysis.model.GiftGrade
import com.santacut.domain.analysis.model.GiftVerdict
import java.util.Objects
import kotlin.random.Random

/**
 * 결론 마지막 한 줄의 펀치 틀 풀(roast/punch.json). 구간(등급)마다의 풀과 판정(GIFT, COAL)마다의 풀을 합쳐 한 사람의 후보로 쓴다.
 * 후보는 참가자·해 시드로 섞어 앞 [suggestions]개를 작가에게 추천한다. 같은 구간이어도 사람마다 다른 펀치가 나와 결론이 판박이가 되지 않게 한다.
 *
 * @property bands 등급 이름([GiftGrade.name])별 펀치 틀
 * @property skins 판정([GiftVerdict.skin])별 펀치 틀
 * @property suggestions 작가에게 추천하는 틀 수
 */
data class RoastPunch(
    val bands: PunchPools,
    val skins: PunchPools,
    val suggestions: Int,
) {
    /** 풀 위반. 등급·판정마다 풀이 있고, 풀마다 표지가 [MIN_STRUCTURES]개 이상이며 유행어 없는 대체 문장이 하나 이상 있어야 한다. */
    fun violations(rules: RoastRules): List<String> = listOfNotNull(
        bands.missingGrades().takeIf(List<String>::isNotEmpty)?.let { names -> "punch.bands: 풀이 없는 등급 $names" },
        skins.missing(SKINS).takeIf(List<String>::isNotEmpty)?.let { names -> "punch.skins: 풀이 없는 판정 $names" },
        "punch.suggestions: ${MIN_SUGGESTIONS}~${MAX_SUGGESTIONS}개다".takeUnless { suggestions in MIN_SUGGESTIONS..MAX_SUGGESTIONS },
    ) + bands.violations(BANDS_FIELD, MIN_STRUCTURES, rules) + skins.violations(SKINS_FIELD, MIN_STRUCTURES, rules)

    /** 판정 [verdict] 구간이 쓸 수 있는 모든 틀(구간 풀과 판정 풀). 배포 전에 대체 문장이 검증 규칙을 통과하는지 테스트가 확인한다. */
    fun poolOf(verdict: GiftVerdict): PunchTemplates = bands.of(verdict.grade().name) + skins.of(verdict.skin())

    /**
     * 판정 [verdict] 구간의 후보를 시드 [seed]로 섞은 순서. 사실 고르기처럼 같은 참가자·같은 해면 같은 순서이고 다른 사람은 다르다.
     * 앞 [suggestions]개가 작가 추천이고, 나머지는 대체 문장이 판정 한 줄과 겹칠 때 넘겨 볼 예비다.
     * 추천 안에는 유행어 없는 틀([PunchTemplate.isPlain])이 늘 하나 이상 든다([PunchTemplates.withPlainWithin]).
     */
    fun candidatesFor(verdict: GiftVerdict, seed: Int, rules: RoastRules): PunchTemplates =
        poolOf(verdict).shuffledBy(Random(Objects.hash(seed, SEED_PURPOSE))).withPlainWithin(suggestions, rules)

    companion object {
        /** 풀 하나에 있어야 하는 서로 다른 펀치 구조(표지) 수. */
        const val MIN_STRUCTURES = 6
        private const val MIN_SUGGESTIONS = 2
        private const val MAX_SUGGESTIONS = 3
        private const val SEED_PURPOSE = "punch"
        private const val BANDS_FIELD = "punch.bands"
        private const val SKINS_FIELD = "punch.skins"
        private val SKINS = listOf(GiftVerdict.GIFT_KEY.uppercase(), GiftVerdict.COAL_KEY.uppercase())

        /** 펀치 틀이 없는 풀. 테스트·옛 설정에서 쓴다. */
        val EMPTY = RoastPunch(PunchPools.EMPTY, PunchPools.EMPTY, MIN_SUGGESTIONS)
    }
}
