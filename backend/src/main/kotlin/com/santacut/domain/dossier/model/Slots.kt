package com.santacut.domain.dossier.model

/**
 * 템플릿 슬롯 이름과 서버가 포맷한 값. `{slot}`은 값으로 바꾸고, `{slot:을}`은 그 값의 끝소리에 맞는 조사만 쓴다.
 * 그래서 템플릿은 `'{phrase}'{phrase:을}`처럼 값 슬롯 바로 뒤에 조사 슬롯을 붙여 쓴다.
 */
class Slots(private val values: Map<String, String>) {
    /** [template]의 모든 슬롯에 값이 있는지. */
    fun canFill(template: String): Boolean = namesIn(template).all { name -> name in values }

    /** 슬롯을 값이나 조사로 바꾼 글. 값이 없는 슬롯이 있으면 [canFill]로 먼저 걸러야 한다. */
    fun fill(template: String): String = SLOT.replace(template) { match -> replacementOf(match) }

    /** 모든 슬롯을 채울 수 있으면 채운 글, 아니면 null이다. */
    fun fillOrNull(template: String): String? = if (canFill(template)) fill(template) else null

    /** 두 슬롯 묶음을 합친다. 같은 이름이면 [other] 값이 이긴다. */
    operator fun plus(other: Slots): Slots = Slots(values + other.values)

    private fun replacementOf(match: MatchResult): String {
        val value = values[match.groupValues[NAME_GROUP]].orEmpty()
        val josa = Josa.of(match.groupValues[JOSA_GROUP]) ?: return value
        return josa.after(value)
    }

    companion object {
        val EMPTY = Slots(emptyMap())
        private const val NAME_GROUP = 1
        private const val JOSA_GROUP = 2

        /** 슬롯 문법. 이름은 영소문자·숫자·밑줄, 조사는 을/를, 이/가, 은/는, 와/과, 으로/로 중 하나다. */
        val SLOT = Regex("\\{([a-z0-9_]+)(?::(을|를|이|가|은|는|와|과|으로|로))?\\}")

        /** 원문·이름·연락처나 놀리면 안 되는 수치로 이어지는 슬롯. copy.json에 등록했더라도 템플릿에 쓰면 앱을 띄우지 않는다. */
        val FORBIDDEN = setOf(
            "name", "nickname", "partner", "partner_name", "speaker", "quote", "text", "message", "raw",
            "url", "link", "phone", "email", "bleep_word", "heavy_word", "heavy_count", "cry_count", "cry_density", "region",
        )

        /** 이름이 null인 값은 뺀다. */
        fun of(vararg entries: Pair<String, String?>): Slots =
            Slots(entries.mapNotNull { (name, value) -> value?.let { name to it } }.toMap())

        /** [template]에 쓴 슬롯 이름. */
        fun namesIn(template: String): List<String> = SLOT.findAll(template).map { match -> match.groupValues[NAME_GROUP] }.toList()
    }
}
