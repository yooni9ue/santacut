package com.santacut.domain.roast.model

/** 저장한 팩폭 문장 하나와 그 출처. */
data class RoastText(val text: String, val source: RoastSource) {
    /** 대체 문장이면 1, 작가 문장이면 0. */
    fun fallbackCount(): Int = if (source == RoastSource.FALLBACK) 1 else 0

    companion object {
        /** 검증을 통과한 작가 문장 [written]이 있으면 그것을, 없으면 대체 문장 [fallback]을 쓴다. */
        fun of(written: String?, fallback: String): RoastText =
            if (written != null) RoastText(written, RoastSource.AI) else RoastText(fallback, RoastSource.FALLBACK)
    }
}
