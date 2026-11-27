package com.santacut.domain.global.config

import com.santacut.domain.roast.model.PhraseFactSpec
import com.santacut.domain.roast.model.RoastCatalog
import com.santacut.domain.roast.model.RoastFactSpecs
import com.santacut.domain.roast.model.RoastFallback
import com.santacut.domain.roast.model.RoastPunch
import com.santacut.domain.roast.model.RoastRules
import com.santacut.domain.roast.model.RoastSlang
import com.santacut.domain.roast.model.ThanksFactSpec
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ResourceLoader
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

/**
 * 앱이 뜰 때 팩폭 작가 리소스(classpath:roast/)를 한 번 읽어 검사한다. 규칙 위반이 하나라도 있으면 앱을 띄우지 않는다.
 * 대체 문장이 검증 규칙(글자 수, 숫자, 금지어)을 지키는지는 배포 전에 테스트가 확인한다.
 */
@Configuration
class RoastCatalogConfiguration {
    @Bean
    fun roastCatalog(resourceLoader: ResourceLoader, objectMapper: ObjectMapper): RoastCatalog =
        readCatalog(objectMapper) { name -> resourceLoader.getResource(LOCATION + name).getContentAsString(Charsets.UTF_8).removePrefix(BOM) }

    companion object {
        private const val LOCATION = "classpath:roast/"
        private const val BOM = "﻿"
        private const val SUPPORTED_VERSION = 1
        private const val VERSION = "version"
        private const val INVALID_MESSAGE = "팩폭 작가 리소스가 규칙에 맞지 않아 앱을 띄우지 않는다: "

        /** 파일 이름으로 내용을 주는 [read]로 다섯 리소스를 읽고 검사한다. 위반이 있으면 [IllegalStateException]이다. 테스트·실측 도구도 이 함수를 쓴다. */
        fun readCatalog(objectMapper: ObjectMapper, read: (String) -> String): RoastCatalog {
            fun node(name: String): JsonNode = objectMapper.readTree(read(name)).also { root -> verifyVersion(name, root) }
            val facts = node("facts.json")
            val catalog = RoastCatalog(
                objectMapper.treeToValue(node("rules.json"), RoastRules::class.java)
                    .withSlang(objectMapper.treeToValue(node("slang.json"), RoastSlang::class.java)),
                objectMapper.treeToValue(facts.path("facts"), RoastFactSpecs::class.java),
                objectMapper.treeToValue(facts.path("phrase"), PhraseFactSpec::class.java),
                objectMapper.treeToValue(facts.path("thanks"), ThanksFactSpec::class.java),
                objectMapper.treeToValue(node("fallback.json"), RoastFallback::class.java),
                objectMapper.treeToValue(node("punch.json"), RoastPunch::class.java),
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
