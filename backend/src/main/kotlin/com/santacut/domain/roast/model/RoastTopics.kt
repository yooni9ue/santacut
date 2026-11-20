package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonValue
import com.santacut.domain.dossier.model.DossierAct
import com.santacut.domain.dossier.model.DossierStats
import com.santacut.domain.dossier.model.TopicEntries

/**
 * 막별 화제 후보. 막마다 1위 화제를 넘기되, 세 막의 1위가 모두 같으면 2위 화제도 함께 넘긴다(세 막 모두 '약속 잡기'가 되지 않게).
 * JSON에서는 배열 하나로 나간다.
 */
class RoastTopics(private val values: List<RoastTopic>) {
    @JsonValue
    fun toList(): List<RoastTopic> = values.toList()

    companion object {
        val EMPTY = RoastTopics(emptyList())
        private const val LEADER_COUNT = 2

        /** [stats]의 주제별 막별 횟수에서 고른다. [excludedKeys] 주제는 넘기지 않는다. */
        fun of(stats: DossierStats, topics: TopicEntries, excludedKeys: List<String>): RoastTopics {
            val leaders = DossierAct.entries.map { act -> topics.leadersOf(act, stats.words.topics, LEADER_COUNT, excludedKeys) }
            val firsts = leaders.map { ranked -> ranked.firstOrNull() }.distinct()
            val sameLeader = firsts.size == 1 && firsts[0] != null
            return RoastTopics(
                DossierAct.entries.mapIndexed { index, act ->
                    RoastTopic(index + 1, act.monthsText(), if (sameLeader) leaders[index] else leaders[index].take(1))
                },
            )
        }
    }
}
