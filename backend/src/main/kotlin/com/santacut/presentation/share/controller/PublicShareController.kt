package com.santacut.presentation.share.controller

import com.santacut.application.share.command.PublicShareCommand
import com.santacut.application.share.facade.ShareFacade
import com.santacut.presentation.share.docs.PublicShareImageApi
import com.santacut.presentation.share.docs.PublicSharePageApi
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

/**
 * 로그인 없이 볼 수 있는 공유 결과 페이지와 카드 이미지.
 */
@RestController
class PublicShareController(
    private val shareFacade: ShareFacade,
) {
    @GetMapping("/s/{token}", produces = [MediaType.TEXT_HTML_VALUE])
    @PublicSharePageApi
    fun page(@PathVariable token: String): ResponseEntity<String> {
        val result = shareFacade.getPublicPage(PublicShareCommand(token))
        return ResponseEntity.ok()
            .header(HttpHeaders.CACHE_CONTROL, NO_STORE)
            .header(REFERRER_POLICY, NO_REFERRER)
            .body(result.html)
    }

    @GetMapping("/s/{token}/image.png", produces = [MediaType.IMAGE_PNG_VALUE])
    @PublicShareImageApi
    fun image(@PathVariable token: String): ResponseEntity<ByteArray> {
        val result = shareFacade.getPublicImage(PublicShareCommand(token))
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, NO_STORE).body(result.png)
    }

    companion object {
        private const val NO_STORE = "no-store"
        private const val REFERRER_POLICY = "Referrer-Policy"
        private const val NO_REFERRER = "no-referrer"
    }
}
