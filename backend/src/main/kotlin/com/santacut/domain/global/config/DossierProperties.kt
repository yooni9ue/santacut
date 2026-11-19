package com.santacut.domain.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 조서 리소스(phrases, topics, kind_words, bleep, heavy, metrics, copy JSON)를 읽을 위치. 끝에 슬래시를 붙인 디렉터리다.
 * 테스트는 콘텐츠 문구에 기대지 않도록 작은 픽스처 디렉터리를 가리킨다.
 */
@ConfigurationProperties(prefix = "santacut.dossier")
data class DossierProperties(
    val resourceLocation: String = "classpath:dossier/",
)
