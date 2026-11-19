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

    /** [key] 어휘. 사전에 없으면 null이다. */
    fun find(key: String): PhraseEntry? = values.find { entry -> entry.key == key }

    /** 사전 순서대로 어휘마다 횟수를 쌓을 빈 칸. */
    fun emptyCounts(): EntryCounts = EntryCounts.ofSize(values.size)

    /** 본인 메시지 본문 [text]에서 어휘마다 센 횟수를 [counts]에 더한다. */
    fun addCountsIn(text: String, counts: EntryCounts) {
        values.forEachIndexed { index, entry -> counts.add(index, entry.forms.countIn(text)) }
    }

    /** [counts]로 쌓은 횟수 중 한 번 이상 나온 어휘. */
    fun hitsOf(counts: EntryCounts): PhraseHits =
        PhraseHits(values.mapIndexed { index, entry -> PhraseHit(entry, counts[index]) }.filter { hit -> hit.count > 0 })
}
