package com.santacut.domain.dossier.model

/**
 * 본인 메시지가 가장 많았던 날의 개수와 그 달(1~12). 날짜는 남기지 않는다.
 */
data class BusiestDay(val count: Int, val month: Int)
