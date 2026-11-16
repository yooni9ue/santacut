package com.santacut.infrastructure.prompt.repository

import com.santacut.domain.prompt.model.Prompt
import org.springframework.data.jpa.repository.JpaRepository

interface PromptRepository : JpaRepository<Prompt, String> {
    /** 이름별 가장 높은 판. 없으면 null이다. */
    fun findFirstByNameOrderByVersionDesc(name: String): Prompt?

    /** 이름의 모든 판. 높은 판부터 나온다. */
    fun findAllByNameOrderByVersionDesc(name: String): List<Prompt>
}
