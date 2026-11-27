package com.santacut.domain.roast.model

/**
 * 근거 카드 종류 하나의 대체 라벨과 한 줄(roast/fallback.json). 통계 카드(말풍선 없이 칩만)에 쓴다.
 * 같은 종류의 카드가 여러 장이면 순서대로 돌려 쓰고, 입버릇 틀([CopyTemplates.PHRASE])은 칩의 입버릇 표기로 채운다.
 */
data class EvidenceCopy(val labels: CopyTemplates, val lines: CopyTemplates) {
    fun isMalformed(): Boolean = labels.lacksPlain() || lines.lacksPlain()

    /** [index]번째 라벨. 입버릇 [phrase]를 넣어도 라벨 상한 안이면 입버릇을 넣은 상 이름('수고'장인상)을 쓴다. */
    fun labelAt(index: Int, phrase: String?): String = labels.pick(index, phrase, RoastTextKind.LABEL.maxLength)

    /** [index]번째 한 줄. 입버릇 [phrase]를 넣고 한 줄 상한 안이면 그 문장을 쓴다. */
    fun lineAt(index: Int, phrase: String?): String = lines.pick(index, phrase, RoastTextKind.EVIDENCE_LINE.maxLength)
}
