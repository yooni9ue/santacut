package com.santacut.domain.dossier.model

/**
 * 3막 화제를 세는 주제 하나(topics.json). [keywords]는 부분 문자열로 센다.
 */
data class TopicEntry(
    val key: String,
    val label: String,
    val keywords: TextForms,
) {
    fun violations(): List<String> = listOfNotNull(
        "topics.$key: label이 비었다".takeIf { label.isBlank() },
        "topics.$key: keywords가 비었거나 빈 키워드가 있다".takeIf { keywords.isMalformed() },
    )
}
