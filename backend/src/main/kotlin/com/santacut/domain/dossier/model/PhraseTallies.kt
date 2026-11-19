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

    /**
     * 지금 사전 [entries]로 다시 읽은 목록. 저장 뒤 사전에서 빠진 말은 빼고, 표기와 분류는 지금 사전 것을 쓴다.
     * 문제가 된 어휘를 사전에서 지우면 배포만으로 모든 조서에서 사라지게 하려는 것이다.
     */
    fun refreshedBy(entries: PhraseEntries): PhraseTallies =
        PhraseTallies(values.mapNotNull { tally -> entries.find(tally.key)?.tally(tally.count) })

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
