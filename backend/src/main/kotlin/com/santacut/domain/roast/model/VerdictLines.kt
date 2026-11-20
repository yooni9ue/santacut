package com.santacut.domain.roast.model

/** 판정(선물·석탄)별 대체 문장 후보(roast/fallback.json). */
data class VerdictLines(val gift: List<String>, val coal: List<String>) {
    fun isMalformed(): Boolean = gift.isEmpty() || coal.isEmpty()

    /** [passed](선물인지)에 맞는 후보 가운데 [seed]로 고른 하나. 같은 평가에는 늘 같은 문장이다. */
    fun pick(passed: Boolean, seed: Int): String {
        val candidates = if (passed) gift else coal
        return candidates[seed.mod(candidates.size)]
    }

    /** 모든 후보. 배포 전에 검증 규칙을 통과하는지 테스트가 확인한다. */
    fun all(): List<String> = gift + coal
}
