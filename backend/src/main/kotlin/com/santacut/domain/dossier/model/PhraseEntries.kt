package com.santacut.domain.dossier.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * 입버릇 허용 어휘 사전(phrases.json의 phrases). JSON에서는 배열 하나로 읽는다.
 */
class PhraseEntries @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(
    private val values: List<PhraseEntry>,
) {
    @JsonValue
    fun toList(): List<PhraseEntry> = values.toList()

    /** 항목마다의 위반과 겹친 key. */
    fun violations(): List<String> =
        values.flatMap(PhraseEntry::violations) +
            values.groupBy(PhraseEntry::key).filterValues { same -> same.size > 1 }.keys.map { key -> "phrases: key $key 중복" }

    /** [text](본인 메시지를 줄바꿈으로 이은 본문)에서 한 번 이상 나온 어휘. */
    fun hitsIn(text: String): PhraseHits =
        PhraseHits(values.map { entry -> PhraseHit(entry, entry.forms.countIn(text)) }.filter { hit -> hit.count > 0 })
}
