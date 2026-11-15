package com.santacut.domain.participant.model

/**
 * 참가자 목록. 순위 계산에서 평가의 참가자를 찾는 데 쓴다.
 */
class Participants(private val values: List<Participant>) {
    fun find(id: String): Participant? = values.firstOrNull { it.id == id }
}
