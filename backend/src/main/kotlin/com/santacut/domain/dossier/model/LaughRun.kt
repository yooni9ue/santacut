package com.santacut.domain.dossier.model

/**
 * 가장 긴 ㅋ 연타의 길이와 그 메시지의 달(1~12)·시(0~23). 분과 날짜는 남기지 않는다.
 */
data class LaughRun(val length: Int, val month: Int, val hour: Int)
