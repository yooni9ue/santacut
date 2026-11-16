package com.santacut.presentation.prompt.controller

import com.santacut.application.prompt.command.PromptNameCommand
import com.santacut.application.prompt.facade.PromptFacade
import com.santacut.presentation.prompt.docs.CreatePromptApi
import com.santacut.presentation.prompt.docs.GetLatestPromptApi
import com.santacut.presentation.prompt.docs.ListPromptVersionsApi
import com.santacut.presentation.prompt.request.CreatePromptRequest
import com.santacut.presentation.prompt.response.PromptResponse
import com.santacut.presentation.prompt.response.PromptVersionResponse
import com.santacut.presentation.prompt.response.PromptVersionResponses
import jakarta.validation.Valid
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * dev 전용 프롬프트 관리 API. `santacut.prompt-admin.enabled=true`일 때만 빈으로 올라온다.
 * 꺼져 있으면(운영 기본값) 이 경로를 처리할 컨트롤러가 없어 404다.
 * 이름 규칙은 도메인의 `PromptName`이 검사하고, 어기면 INVALID_REQUEST 400이다.
 */
@RestController
@ConditionalOnProperty(prefix = "santacut.prompt-admin", name = ["enabled"], havingValue = "true")
class PromptAdminController(
    private val promptFacade: PromptFacade,
) {
    @PostMapping(PROMPT_PATH)
    @CreatePromptApi
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @PathVariable name: String,
        @Valid @RequestBody request: CreatePromptRequest,
    ): PromptVersionResponse = PromptVersionResponse.from(promptFacade.create(request.toCommand(name)))

    @GetMapping(PROMPT_PATH)
    @GetLatestPromptApi
    fun latest(@PathVariable name: String): PromptResponse =
        PromptResponse.from(promptFacade.getLatest(PromptNameCommand(name)))

    @GetMapping("$PROMPT_PATH/versions")
    @ListPromptVersionsApi
    fun versions(@PathVariable name: String): PromptVersionResponses =
        PromptVersionResponses.from(promptFacade.listVersions(PromptNameCommand(name)))

    companion object {
        private const val PROMPT_PATH = "/v1/admin/prompts/{name}"
    }
}
