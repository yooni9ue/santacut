package com.santacut.domain.share.model

/** 공유한 줄거리 카드의 막 하나. [months]는 "1~4월" 같은 달 범위다. */
data class RecapDeckAct(val act: Int, val months: String, val line: String)
