package com.santacut.domain.dossier.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * copy.json `slots`에 등록한 슬롯 하나. [format]과 [usedIn]은 작가와 엔진이 맞춰 보는 설명이고, 엔진은 [name]만 허용 목록으로 쓴다.
 */
data class SlotSpec(
    val name: String,
    val format: String = "",
    @JsonProperty("used_in") val usedIn: List<String> = emptyList(),
)
