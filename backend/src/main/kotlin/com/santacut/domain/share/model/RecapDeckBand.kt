package com.santacut.domain.share.model

/** 공유한 컷 표 카드의 구간 한 칸. [current]면 내 점수가 든 칸이다. */
data class RecapDeckBand(val label: String, val range: String, val current: Boolean)
