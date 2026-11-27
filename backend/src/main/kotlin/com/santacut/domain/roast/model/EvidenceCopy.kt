package com.santacut.domain.roast.model

/**
 * 근거 카드 종류 하나의 대체 라벨과 한 줄(roast/fallback.json의 evidence). 통계 카드(말풍선 없이 칩만)에 쓴다.
 * 같은 종류의 카드가 여러 장이면 순서대로 돌려 쓴다.
 */
data class EvidenceCopy(val labels: List<String>, val lines: List<String>) {
    fun isMalformed(): Boolean = labels.isEmpty() || lines.isEmpty()

    fun labelAt(index: Int): String = labels[index.mod(labels.size)]

    fun lineAt(index: Int): String = lines[index.mod(lines.size)]
}
