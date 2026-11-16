package com.santacut.application.prompt.command

/**
 * 프롬프트의 다음 판을 만든다.
 *
 * @property name 프롬프트 이름
 * @property content 새 판의 본문
 */
data class CreatePromptCommand(
    val name: String,
    val content: String,
)
