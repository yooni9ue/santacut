package com.santacut.domain.dossier.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * 저장한 입버릇 TOP 목록. 순서가 순위다. JSON에서는 배열 하나로 읽고 쓴다.
 */
class PhraseTallies @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(
    private val values: List<PhraseTally>,
) {
    @JsonValue
    fun toList(): List<PhraseTally> = values.toList()

    fun isEmpty(): Boolean = values.isEmpty()

    /** [rank]위(1부터) 말. 없으면 null이다. */
    fun at(rank: Int): PhraseTally? = values.getOrNull(rank - 1)

    /** 장부 줄. '1위 진짜 2,104번'처럼 순위, 표기, 횟수만 적고 [limit]개까지다. 공유할 수 있다. */
    fun ledgerLines(limit: Int): List<LineDraft> = values.take(limit).mapIndexed { index, tally ->
        LineDraft(DossierFormat.rank(index + 1) + SPACE + tally.display + SPACE + DossierFormat.times(tally.count), true)
    }

    companion object {
        private const val SPACE = " "
    }
}
