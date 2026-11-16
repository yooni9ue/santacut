package com.santacut.presentation.share.request

import com.fasterxml.jackson.annotation.JsonProperty
import com.santacut.application.share.command.CreateShareCommand
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

data class CreateShareRequest(
    @field:JsonProperty("include_quote")
    @field:Schema(
        description = "true면 결과의 본인 한마디(quote)를 공유 카드에 싣습니다. 기본은 false입니다. 상대 말은 어떤 경우에도 싣지 않습니다.",
        defaultValue = "false",
    )
    val includeQuote: Boolean = false,
) {
    fun toCommand(participantId: UUID): CreateShareCommand = CreateShareCommand(participantId, includeQuote)
}
