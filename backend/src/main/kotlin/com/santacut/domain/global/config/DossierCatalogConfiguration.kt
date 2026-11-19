package com.santacut.domain.global.config

import com.santacut.domain.dossier.model.DossierCatalog
import com.santacut.domain.dossier.model.DossierCopy
import com.santacut.domain.dossier.model.HeavyWords
import com.santacut.domain.dossier.model.KindWords
import com.santacut.domain.dossier.model.MetricSpecs
import com.santacut.domain.dossier.model.PhraseEntries
import com.santacut.domain.dossier.model.TextForms
import com.santacut.domain.dossier.model.TopicEntries
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ResourceLoader
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

/**
 * 앱이 뜰 때 조서 리소스를 한 번 읽어 검사한다. 금지·미등록 슬롯, 템플릿 안의 아라비아 숫자, 극값 문구가 없는 지표,
 * 모르는 채점 항목 같은 위반이 하나라도 있으면 앱을 띄우지 않는다. 잘못된 문구가 사용자에게 나가기 전에 배포에서 막으려는 것이다.
 */
@Configuration
class DossierCatalogConfiguration {
    @Bean
    fun dossierCatalog(properties: DossierProperties, resourceLoader: ResourceLoader, objectMapper: ObjectMapper): DossierCatalog =
        readCatalog(objectMapper) { name ->
            resourceLoader.getResource(properties.resourceLocation + name).getContentAsString(Charsets.UTF_8).removePrefix(BOM)
        }

    companion object {
        private const val BOM = "﻿"
        private const val SUPPORTED_VERSION = 1
        private const val VERSION = "version"
        private const val INVALID_MESSAGE = "조서 리소스가 규칙에 맞지 않아 앱을 띄우지 않는다: "

        /** 파일 이름으로 내용을 주는 [read]로 일곱 리소스를 읽고 검사한다. 위반이 있으면 [IllegalStateException]이다. */
        fun readCatalog(objectMapper: ObjectMapper, read: (String) -> String): DossierCatalog {
            fun node(name: String): JsonNode = objectMapper.readTree(read(name)).also { root -> verifyVersion(name, root) }
            val catalog = DossierCatalog(
                objectMapper.treeToValue(node("phrases.json").path("phrases"), PhraseEntries::class.java),
                objectMapper.treeToValue(node("topics.json").path("topics"), TopicEntries::class.java),
                objectMapper.treeToValue(node("kind_words.json").path("kinds"), KindWords::class.java),
                objectMapper.treeToValue(node("bleep.json").path("forms"), TextForms::class.java),
                objectMapper.treeToValue(node("heavy.json"), HeavyWords::class.java),
                objectMapper.treeToValue(node("metrics.json").path("metrics"), MetricSpecs::class.java),
                objectMapper.treeToValue(node("copy.json"), DossierCopy::class.java),
            )
            val violations = catalog.violations()
            if (violations.isNotEmpty()) throw IllegalStateException(INVALID_MESSAGE + violations.joinToString("; "))
            return catalog
        }

        private fun verifyVersion(name: String, root: JsonNode) {
            if (root.path(VERSION).asInt() != SUPPORTED_VERSION) throw IllegalStateException("$INVALID_MESSAGE$name version은 1이어야 한다")
        }
    }
}
