package com.santacut.infrastructure.render

import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component

/**
 * 공유 결과를 Open Graph 메타데이터가 있는 HTML 페이지로 그린다.
 * 페이지 틀은 `share/share-page.html` 템플릿에 있고, `{{이름}}` 자리표시자를 한 번에 바꿔 넣는다.
 */
@Component
class SharePageRenderer {
    private val template: String by lazy {
        ClassPathResource(TEMPLATE_PATH).inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }.trimEnd()
    }

    fun render(card: ShareCard, publicOrigin: String): String {
        val view = SharePageView.of(card, publicOrigin)
        return PLACEHOLDER.replace(template) { view.valueOf(it.groupValues[1]) }
    }

    companion object {
        private const val TEMPLATE_PATH = "share/share-page.html"
        private val PLACEHOLDER = Regex("\\{\\{(\\w+)}}")
    }
}
