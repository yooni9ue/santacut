package com.santacut.infrastructure.share.render

import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component

/**
 * 공유 결과를 Open Graph 메타데이터가 있는 HTML 페이지로 그린다.
 * 페이지 틀은 `share/` 아래 템플릿에 있고, `{{이름}}` 자리표시자를 한 번에 바꿔 넣는다.
 */
@Component
class SharePageRenderer {
    private val template: String by lazy { load(TEMPLATE_PATH) }
    private val inactiveTemplate: String by lazy { load(INACTIVE_TEMPLATE_PATH) }

    fun render(card: ShareCard, publicOrigin: String): String = fill(template, SharePageView.of(card, publicOrigin))

    /** 공유 링크가 없거나 취소됐을 때 보여 줄 페이지. 서비스 시작 링크만 담는다. */
    fun renderInactive(publicOrigin: String): String = fill(inactiveTemplate, SharePageView.inactive(publicOrigin))

    private fun fill(page: String, view: SharePageView): String =
        PLACEHOLDER.replace(page) { view.valueOf(it.groupValues[1]) }

    private fun load(path: String): String =
        ClassPathResource(path).inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }.trimEnd()

    companion object {
        private const val TEMPLATE_PATH = "share/share-page.html"
        private const val INACTIVE_TEMPLATE_PATH = "share/share-inactive.html"
        private val PLACEHOLDER = Regex("\\{\\{(\\w+)}}")
    }
}
