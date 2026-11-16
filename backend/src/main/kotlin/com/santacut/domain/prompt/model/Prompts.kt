package com.santacut.domain.prompt.model

/**
 * 같은 이름 프롬프트의 판 목록. 리포지토리가 준 순서(판 번호 내림차순)를 유지한다.
 */
class Prompts(private val values: List<Prompt>) {
    fun isEmpty(): Boolean = values.isEmpty()

    /** 목록 순서대로 각 판을 바꾼 결과. */
    fun <T> map(transform: (Prompt) -> T): List<T> = values.map(transform)
}
