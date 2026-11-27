package com.santacut.domain.roast.model

/**
 * 결론 마지막 한 줄(final_line)의 펀치 틀 하나(roast/punch.json). 작가에게는 [template]만 추천으로 보내고, 서버는 [marker]로 그 틀을 썼는지 본다.
 * 작가 문장이 끝내 검증을 통과하지 못하면 [fallback]을 대체 마지막 한 줄로 쓴다.
 *
 * @property template 작가에게 보내는 틀. 괄호 빈칸((버릇), (본인 말) 등)은 작가가 이 사람의 말이나 버릇으로 채운다.
 * @property marker 이 틀을 가려내는 표지. 허용 유행어 줄기나 구간 펀치 틀 표지 가운데 하나다(체포하겠, 딸깍, 프리패스, 야~ 등).
 * @property fallback 빈칸을 채운 대체 마지막 한 줄. 검증 규칙(글자 수, 숫자, 판정 방향 등)을 지켜야 한다.
 */
data class PunchTemplate(val template: String, val marker: String, val fallback: String) {
    /** 틀과 대체 문장이 [rules]의 펀치 표지로 [marker]를 품었는지 본 위반. [where]는 위반 설명 머리말이다. */
    fun violations(where: String, rules: RoastRules): List<String> = listOfNotNull(
        "$where: template·marker·fallback이 비었다".takeIf { template.isBlank() || marker.isBlank() || fallback.isBlank() },
        "$where: template에 펀치 표지 $marker 가 없다".takeUnless { marker in rules.punchMarksIn(template) },
        "$where: fallback에 펀치 표지 $marker 가 없다".takeUnless { marker in rules.punchMarksIn(fallback) },
    )

    /** 유행어 없는 틀인지. 틀과 대체 문장 모두 허용 유행어를 쓰지 않는다(표지가 프리패스·이야~ 같은 구간 펀치 틀 표지다). */
    fun isPlain(rules: RoastRules): Boolean = rules.slangCountIn(template) == 0 && rules.slangCountIn(fallback) == 0

    /** [text]가 이 틀을 썼는지. 따옴표 인용 밖에 [marker]가 있어야 한다. */
    fun isUsedIn(text: String, rules: RoastRules): Boolean = marker in rules.punchMarksIn(text)
}
