package com.santacut.domain.dossier.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.santacut.domain.analysis.model.ScoreDimension

/**
 * 따뜻한 말 사전(kind_words.json의 kinds). 채점 항목마다 하나 이상 있어야 판결 사유를 만들 수 있다.
 */
class KindWords @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(
    private val values: List<KindWord>,
) {
    @JsonValue
    fun toList(): List<KindWord> = values.toList()

    fun violations(): List<String> =
        values.flatMap(KindWord::violations) +
            values.groupBy(KindWord::key).filterValues { same -> same.size > 1 }.keys.map { key -> "kind_words: key $key 중복" } +
            ScoreDimension.entries.filter { dimension -> values.none { kind -> kind.belongsTo(dimension) } }
                .map { dimension -> "kind_words: ${dimension.key} 항목의 말이 없다" }

    /** [text](본인 본문)에서 센 말별 횟수. 한 번도 안 쓴 말은 뺀다. */
    fun countIn(text: String): KindCounts =
        KindCounts(values.associate { kind -> kind.key to kind.forms.countIn(text) }.filterValues { count -> count > 0 })

    /** [dimension] 항목의 말 중 가장 많이 쓴 말. 같으면 사전 순서가 앞선 말이다. 하나도 안 썼으면 null이다. */
    fun strongestIn(dimension: ScoreDimension, counts: KindCounts): KindTally? = values
        .filter { kind -> kind.belongsTo(dimension) }
        .map { kind -> KindTally(kind.key, kind.label, counts.countOf(kind.key)) }
        .filter { tally -> tally.count > 0 }
        .maxByOrNull { tally -> tally.count }

    /** 쓴 적 있는 말의 key를 많이 쓴 순서로. 같으면 사전 순서다. */
    fun keysByCount(counts: KindCounts): List<String> = values
        .filter { kind -> counts.countOf(kind.key) > 0 }
        .sortedByDescending { kind -> counts.countOf(kind.key) }
        .map(KindWord::key)

    /** [key] 말의 이름. 사전에 없으면 null이다. */
    fun labelOf(key: String): String? = values.find { kind -> kind.key == key }?.label
}
