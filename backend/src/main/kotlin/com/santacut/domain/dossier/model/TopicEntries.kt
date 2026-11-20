package com.santacut.domain.dossier.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * 주제 사전(topics.json의 topics). JSON에서는 배열 하나로 읽는다.
 */
class TopicEntries @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(
    private val values: List<TopicEntry>,
) {
    @JsonValue
    fun toList(): List<TopicEntry> = values.toList()

    fun violations(): List<String> =
        values.flatMap(TopicEntry::violations) +
            values.groupBy(TopicEntry::key).filterValues { same -> same.size > 1 }.keys.map { key -> "topics: key $key 중복" }

    /** 주제마다 막별 횟수를 쌓을 빈 칸(주제 순서 × 막 순서). */
    fun emptyCounts(): EntryCounts = EntryCounts.ofSize(values.size * ACTS)

    /** [act] 막에 보낸 본인 메시지 본문 [text]에서 주제마다 센 횟수를 [counts]에 더한다. */
    fun addCountsIn(text: String, act: DossierAct, counts: EntryCounts) {
        values.forEachIndexed { index, topic -> counts.add(index * ACTS + act.ordinal, topic.keywords.countIn(text)) }
    }

    /** [counts]로 쌓은 주제별 막별 횟수. 한 번도 안 나온 주제는 뺀다. */
    fun countsOf(counts: EntryCounts): TopicActCounts = TopicActCounts(
        values.withIndex()
            .associate { (index, topic) -> topic.key to List(ACTS) { act -> counts[index * ACTS + act] } }
            .filterValues { actCounts -> actCounts.sum() > 0 },
    )

    /** [act] 막의 화제 1위. 횟수가 같으면 사전 순서가 앞선 주제다. 그 막에 나온 주제가 없으면 null이다. */
    fun leaderOf(act: DossierAct, counts: TopicActCounts): ActTopic? = values
        .map { topic -> ActTopic(topic.label, counts.countOf(topic.key, act)) }
        .filter { topic -> topic.count > 0 }
        .maxByOrNull { topic -> topic.count }

    /**
     * [act] 막의 화제 상위 [limit]개 이름. 횟수가 같으면 사전 순서가 앞선 주제다. [excludedKeys] 주제와 그 막에 안 나온 주제는 뺀다.
     */
    fun leadersOf(act: DossierAct, counts: TopicActCounts, limit: Int, excludedKeys: List<String>): List<String> = values
        .filterNot { topic -> topic.key in excludedKeys }
        .map { topic -> ActTopic(topic.label, counts.countOf(topic.key, act)) }
        .filter { topic -> topic.count > 0 }
        .sortedByDescending { topic -> topic.count }
        .take(limit)
        .map(ActTopic::label)

    /**
     * 막마다 화제 1위 줄. 화제가 없는 막은 노출 구간이 허락할 때만 빈 막 줄을 쓴다([DossierExposure.showsEmptyActs]).
     */
    fun actLines(context: SceneContext): List<LineDraft> =
        DossierAct.entries.mapNotNull { act -> actLine(act, leaderOf(act, context.stats.words.topics), context) }

    private fun actLine(act: DossierAct, leader: ActTopic?, context: SceneContext): LineDraft? {
        val copy = context.copy().acts
        val slots = context.slots + Slots.of(ACT_LABEL to act.label)
        val seed = context.seed.of(ACTS_PURPOSE + act.name)
        if (leader == null) return if (context.exposure.showsEmptyActs) copy.empty.pick(seed, slots)?.let { text -> LineDraft(text, true) } else null
        val topicSlots = Slots.of(TOPIC to leader.label, COUNT to DossierFormat.times(leader.count))
        return copy.line.pick(seed, slots + topicSlots)?.let { text -> LineDraft(text, true) }
    }

    companion object {
        private val ACTS = DossierAct.entries.size
        private const val ACT_LABEL = "act_label"
        private const val TOPIC = "topic"
        private const val COUNT = "count"
        private const val ACTS_PURPOSE = "acts."
    }
}
