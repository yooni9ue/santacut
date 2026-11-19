package com.santacut.domain.dossier.model

/**
 * 이모지별 본인 사용 횟수. 처음 나온 순서를 지켜 횟수가 같으면 먼저 나온 이모지가 앞선다.
 */
class EmojiCounts(private val values: MutableMap<String, Int> = LinkedHashMap()) {
    /** [emojis]를 하나씩 더하고 더한 개수를 돌려준다. */
    fun addAll(emojis: List<String>): Int {
        emojis.forEach { emoji -> values.merge(emoji, 1, Int::plus) }
        return emojis.size
    }

    /** 많이 쓴 [limit]개. */
    fun top(limit: Int): EmojiTallies = EmojiTallies(
        values.entries.sortedByDescending { entry -> entry.value }.take(limit).map { entry -> EmojiTally(entry.key, entry.value) },
    )
}
