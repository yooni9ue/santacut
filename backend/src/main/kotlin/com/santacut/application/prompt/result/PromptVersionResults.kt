package com.santacut.application.prompt.result

import com.santacut.domain.prompt.model.Prompts

/**
 * 프롬프트 판 목록. 높은 판부터 나온다.
 */
class PromptVersionResults(private val values: List<PromptVersionResult>) {
    /** 목록 순서대로 각 판 결과를 바꾼 결과. */
    fun <T> map(transform: (PromptVersionResult) -> T): List<T> = values.map(transform)

    companion object {
        fun from(prompts: Prompts): PromptVersionResults = PromptVersionResults(prompts.map(PromptVersionResult::from))
    }
}
