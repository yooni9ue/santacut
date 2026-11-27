package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 칩 하나(또는 같은 머리말 칩 묶음)의 대체 라벨과 한 줄(roast/fallback.json의 chips). 통계 카드 문구가 그 칩을 말하게 한다.
 * 근거 종류(이유, 깎인 말, 살아남은 말)마다 따로 두어 카드 온도를 맞춘다.
 *
 * @property keys 이 문구를 쓰는 칩 key
 * @property prefix 이 머리말로 시작하는 칩 key에도 쓴다(입버릇 칩 `phrase_`). 없으면 null이다.
 * @property mildCut 높은 점수 가운데 가장 낮아 억지로 깎인 말이 된 칩(컷 이상 항목 점수)의 순한 깎인 말 문구. 없으면 null이다.
 */
data class ChipCopy(
    val keys: List<String> = emptyList(),
    val prefix: String? = null,
    val reason: EvidenceCopy,
    val cut: EvidenceCopy,
    val keep: EvidenceCopy,
    @JsonProperty("mild_cut") val mildCut: EvidenceCopy? = null,
) {
    /** [chipKey] 칩에 이 문구를 쓰는지. */
    fun covers(chipKey: String): Boolean = chipKey in keys || (prefix != null && chipKey.startsWith(prefix))

    /** [kind] 문구. [mild]이고 순한 깎인 말 문구가 있으면 깎인 말에 그것을 쓴다. */
    fun copyOf(kind: EvidenceKind, mild: Boolean = false): EvidenceCopy = when (kind) {
        EvidenceKind.REASON -> reason
        EvidenceKind.CUT -> mildCut?.takeIf { mild } ?: cut
        EvidenceKind.KEEP -> keep
    }

    fun isMalformed(): Boolean =
        (keys.isEmpty() && prefix.isNullOrBlank()) || reason.isMalformed() || cut.isMalformed() || keep.isMalformed() || mildCut?.isMalformed() == true
}
