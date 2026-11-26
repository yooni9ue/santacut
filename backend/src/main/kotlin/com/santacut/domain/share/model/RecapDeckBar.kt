package com.santacut.domain.share.model

import java.math.BigDecimal

/** 공유한 항목 막대 카드의 막대 하나. [score]는 0~100점이다. */
data class RecapDeckBar(val label: String, val score: BigDecimal)
