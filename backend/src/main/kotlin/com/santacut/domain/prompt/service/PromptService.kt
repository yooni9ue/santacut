package com.santacut.domain.prompt.service

import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import com.santacut.domain.prompt.model.Prompt
import com.santacut.domain.prompt.model.Prompts
import com.santacut.infrastructure.prompt.repository.PromptRepository
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class PromptService(
    private val promptRepository: PromptRepository,
) {
    /** 이름별 가장 높은 판. 하나도 없으면 [ErrorCode.PROMPT_NOT_FOUND]다. */
    fun latest(name: String): Prompt =
        promptRepository.findFirstByNameOrderByVersionDesc(name) ?: throw SantaCutException(ErrorCode.PROMPT_NOT_FOUND, name)

    /**
     * 가장 높은 판 다음 번호로 새 판을 저장한다. 처음 쓰는 이름이면 1판이다.
     *
     * 커넥션 풀이 하나(SQLite)라 읽기와 쓰기 사이에 다른 트랜잭션이 끼지 못한다.
     * 그래도 (name, version) 유니크 제약이 같은 번호의 두 번째 저장을 막는다.
     */
    fun create(name: String, content: String, createdAt: Instant): Prompt {
        val latest = promptRepository.findFirstByNameOrderByVersionDesc(name)
        val prompt = latest?.next(content, createdAt) ?: Prompt.initial(name, content, createdAt)
        return promptRepository.saveAndFlush(prompt)
    }

    /** 이름의 모든 판(높은 판부터). 하나도 없으면 [ErrorCode.PROMPT_NOT_FOUND]다. */
    fun versions(name: String): Prompts {
        val prompts = Prompts(promptRepository.findAllByNameOrderByVersionDesc(name))
        if (prompts.isEmpty()) throw SantaCutException(ErrorCode.PROMPT_NOT_FOUND, name)
        return prompts
    }
}
