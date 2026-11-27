package com.santacut.domain.roast.model

/**
 * 칩 하나(또는 같은 머리말 칩 묶음)의 대체 라벨과 한 줄(roast/fallback.json의 chips). 통계 카드 문구가 그 칩을 말하게 한다.
 * 근거 종류(이유, 깎인 말, 살아남은 말)마다 따로 두어 카드 온도를 맞춘다.
 *
 * @property keys 이 문구를 쓰는 칩 key
 * @property prefix 이 머리말로 시작하는 칩 key에도 쓴다(입버릇 칩 `phrase_`). 없으면 null이다.
 */
data class ChipCopy(
    val keys: List<String> = emptyList(),
    val prefix: String? = null,
    val reason: EvidenceCopy,
    val cut: EvidenceCopy,
    val keep: EvidenceCopy,
) {
    /** [chipKey] 칩에 이 문구를 쓰는지. */
    fun covers(chipKey: String): Boolean = chipKey in keys || (prefix != null && chipKey.startsWith(prefix))

    fun copyOf(kind: EvidenceKind): EvidenceCopy = when (kind) {
        EvidenceKind.REASON -> reason
        EvidenceKind.CUT -> cut
        EvidenceKind.KEEP -> keep
    }

    fun isMalformed(): Boolean = (keys.isEmpty() && prefix.isNullOrBlank()) || reason.isMalformed() || cut.isMalformed() || keep.isMalformed()
}
