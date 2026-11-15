package com.santacut.presentation.controller

import com.santacut.application.command.CreateShareCommand
import com.santacut.application.command.RevokeShareCommand
import com.santacut.application.facade.ShareFacade
import com.santacut.presentation.docs.CreateShareApi
import com.santacut.presentation.docs.RevokeShareApi
import com.santacut.presentation.response.CreateShareResponse
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class ShareController(
    private val shareFacade: ShareFacade,
) {
    @PostMapping("/v1/shares")
    @CreateShareApi
    fun create(@AuthenticationPrincipal @Parameter(hidden = true) participantId: UUID): CreateShareResponse =
        CreateShareResponse.from(shareFacade.createShare(CreateShareCommand(participantId)))

    @DeleteMapping("/v1/shares/{token}")
    @RevokeShareApi
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun revoke(
        @PathVariable token: String,
        @AuthenticationPrincipal @Parameter(hidden = true) participantId: UUID,
    ): ResponseEntity<Void> {
        shareFacade.revokeShare(RevokeShareCommand(participantId, token))
        return ResponseEntity.noContent().build()
    }
}
