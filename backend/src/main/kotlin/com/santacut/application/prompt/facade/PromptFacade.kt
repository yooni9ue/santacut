package com.santacut.application.prompt.facade

import com.santacut.application.global.Facade
import com.santacut.application.prompt.command.CreatePromptCommand
import com.santacut.application.prompt.command.PromptNameCommand
import com.santacut.application.prompt.result.PromptResult
import com.santacut.application.prompt.result.PromptVersionResult
import com.santacut.application.prompt.result.PromptVersionResults
import com.santacut.domain.prompt.service.PromptService
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

/**
 * 관리자 프롬프트 API의 유스케이스. 로그에는 이름과 판 번호만 남기고 본문은 남기지 않는다.
 */
@Facade
class PromptFacade(
    private val promptService: PromptService,
    private val clock: Clock,
) {
    private val logger = LoggerFactory.getLogger(PromptFacade::class.java)

    /** 가장 높은 판 다음 번호로 새 판을 저장한다. 읽기와 쓰기를 한 트랜잭션에서 한다. */
    @Transactional
    fun create(command: CreatePromptCommand): PromptVersionResult {
        val prompt = promptService.create(command.name, command.content, clock.instant())
        logger.info("프롬프트 {} {}판을 저장했다", prompt.name, prompt.version)
        return PromptVersionResult.from(prompt)
    }

    @Transactional(readOnly = true)
    fun getLatest(command: PromptNameCommand): PromptResult = PromptResult.from(promptService.latest(command.name))

    @Transactional(readOnly = true)
    fun listVersions(command: PromptNameCommand): PromptVersionResults =
        PromptVersionResults.from(promptService.versions(command.name))
}
