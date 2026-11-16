package com.santacut.application.prompt.command

/**
 * 이름 하나로 프롬프트를 조회한다. 최신 판 조회와 판 목록 조회가 함께 쓴다.
 *
 * @property name 프롬프트 이름
 */
data class PromptNameCommand(
    val name: String,
)
