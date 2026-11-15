package com.santacut.presentation.response

import com.santacut.application.result.ShareLinkResult
import io.swagger.v3.oas.annotations.media.Schema

data class CreateShareResponse(
    @field:Schema(description = "43자 base64url 공유 토큰. 토큰을 아는 사람은 누구나 결과를 볼 수 있으니 함부로 노출하지 않습니다.")
    val token: String,
    @field:Schema(description = "공개 HTML 결과 페이지 주소", format = "uri")
    val url: String,
    @field:Schema(description = "공개 1200×630 PNG 카드 주소", format = "uri")
    val imageUrl: String,
) {
    companion object {
        fun from(result: ShareLinkResult): CreateShareResponse =
            CreateShareResponse(result.token, result.url, result.imageUrl)
    }
}
