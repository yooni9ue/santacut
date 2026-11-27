package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import kotlin.random.Random

/**
 * 펀치 틀 목록(roast/punch.json의 구간·판정별 풀), 또는 한 사람에게 시드 순서로 줄 세운 후보. JSON에서는 배열 하나로 읽고 쓴다.
 * 같은 표지([PunchTemplate.marker])의 틀이 앞에 있으면 뒤의 것은 후보에서 빠진다. 같은 펀치를 두 번 추천하지 않으려는 것이다.
 */
class PunchTemplates @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(
    private val values: List<PunchTemplate>,
) {
    @JsonValue
    fun toList(): List<PunchTemplate> = values.toList()

    fun size(): Int = values.size

    /** 서로 다른 표지 수. 풀마다 [RoastPunch.MIN_STRUCTURES]개 이상이어야 한다. */
    fun markerCount(): Int = values.map(PunchTemplate::marker).distinct().size

    /** 대체 문장에 허용 유행어가 없는 틀이 있는지. 덱 전체 유행어 상한을 넘을 때 쓸 펀치가 있어야 한다. */
    fun hasPlain(rules: RoastRules): Boolean = values.any { punch -> rules.slangCountIn(punch.fallback) == 0 }

    /** 틀마다의 위반. [where]는 풀 이름이다. */
    fun violations(where: String, rules: RoastRules): List<String> =
        values.withIndex().flatMap { (index, punch) -> punch.violations("$where[$index]", rules) }

    /** 대체 문장 전체. 배포 전에 검증 규칙을 통과하는지 테스트가 확인한다. */
    fun fallbacks(): List<String> = values.map(PunchTemplate::fallback)

    operator fun plus(other: PunchTemplates): PunchTemplates = PunchTemplates(values + other.values)

    /** [random]으로 섞고 표지가 겹치는 틀을 뺀 후보 순서. 같은 시드면 늘 같은 순서다. */
    fun shuffledBy(random: Random): PunchTemplates = PunchTemplates(values.shuffled(random).distinctBy(PunchTemplate::marker))

    /** 앞 [count]개(작가에게 보내는 추천 틀). */
    fun take(count: Int): PunchTemplates = PunchTemplates(values.take(count))

    /** 작가 입력용 틀 글. */
    fun hints(): List<String> = values.map(PunchTemplate::template)

    /** 표지 목록. 다시 쓰기 사유에 싣는다. */
    fun markers(): List<String> = values.map(PunchTemplate::marker)

    /** [text]가 이 목록의 틀 가운데 하나를 썼는지. 목록이 비었으면 참이다(추천이 없으면 따지지 않는다). */
    fun anyUsedIn(text: String, rules: RoastRules): Boolean = values.isEmpty() || values.any { punch -> punch.isUsedIn(text, rules) }

    /**
     * 대체 마지막 한 줄. 후보 순서대로 판정 한 줄 [verdictLine]과 펀치 표지·유행어가 겹치지 않는 첫 틀의 대체 문장이다.
     * [plain]이면 허용 유행어가 없는 대체 문장만 고른다. 맞는 틀이 없으면 null이다.
     */
    fun fallbackFor(verdictLine: String, plain: Boolean, rules: RoastRules): String? = values.firstOrNull { punch ->
        rules.punchClashIn(verdictLine, punch.fallback) == null && (!plain || rules.slangCountIn(punch.fallback) == 0)
    }?.fallback

    companion object {
        val EMPTY = PunchTemplates(emptyList())
    }
}
