package com.santacut.domain.dossier.model

import com.santacut.domain.analysis.model.ScoreVerdict

/**
 * 총평을 고르는 판정 구간. 기존 점수 판정([ScoreVerdict])을 그대로 네 구간으로 옮긴다.
 */
enum class VerdictBand(val key: String) {
    TOP("top"),
    HIGH("high"),
    MID("mid"),
    LOW("low"),
    ;

    fun isLow(): Boolean = this == LOW

    companion object {
        /** copy.json `dossier.summary_by_band`에 있어야 하는 key. */
        val KEYS = listOf(TOP.key, HIGH.key, MID.key, LOW.key)

        fun of(verdict: ScoreVerdict): VerdictBand = when (verdict) {
            ScoreVerdict.SANTA_FAMILY -> TOP
            ScoreVerdict.GIFT_DEPARTED -> HIGH
            ScoreVerdict.DELIVERY_ON_HOLD -> MID
            ScoreVerdict.COAL_ON_DELIVERY -> LOW
        }
    }
}
