package com.santacut.domain.roast.model

/**
 * 작가 응답 필드 하나의 검증 결과. [field]는 title, verdict, verdict_mild, letter, pairs[0]~pairs[4], story[0]~story[2],
 * pairs(짝 개수), story(줄 개수), json(읽을 수 없는 응답) 중 하나다. [reasons]가 비어 있으면 통과다.
 */
data class RoastCheck(val field: String, val reasons: List<String>) {
    val passed: Boolean = reasons.isEmpty()
}
