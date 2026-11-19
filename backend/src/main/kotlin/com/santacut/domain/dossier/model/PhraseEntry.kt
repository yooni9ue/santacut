package com.santacut.domain.dossier.model

import com.fasterxml.jackson.annotation.JsonProperty
import kotlin.math.ln

/**
 * 입버릇 허용 어휘 하나(phrases.json). 이 목록에 있는 말만 화면과 저장에 쓴다.
 *
 * @property baselinePer100 보통 사람의 본인 메시지 100개당 예상 사용 횟수. 특이도의 기준이다.
 * @property category 말의 분류. 장부 메모를 고를 때 쓴다.
 */
data class PhraseEntry(
    val key: String,
    val display: String,
    val forms: TextForms,
    @JsonProperty("baseline_per_100") val baselinePer100: Double,
    val category: String,
) {
    /** 규칙에 맞지 않는 곳의 설명. 맞으면 빈 목록이다. */
    fun violations(): List<String> = listOfNotNull(
        "phrases.$key: key는 영소문자, 숫자, 밑줄만 쓴다".takeUnless { KEY.matches(key) },
        "phrases.$key: display가 비었다".takeIf { display.isBlank() },
        "phrases.$key: forms가 비었거나 빈 표면형이 있다".takeIf { forms.isMalformed() },
        "phrases.$key: baseline_per_100은 0 이상이다".takeIf { baselinePer100 < 0 },
        "phrases.$key: 모르는 category $category".takeUnless { category in CATEGORIES },
    )

    /** 본인 메시지 [messageCount]개에서 [count]번 쓴 이 말의 특이도. 0.5를 더해 드물게 쓴 말이 튀지 않게 한다. */
    fun specificity(count: Int, messageCount: Int): Double =
        ln((count * PER_HUNDRED / messageCount + SMOOTHING) / (baselinePer100 + SMOOTHING))

    fun tally(count: Int): PhraseTally = PhraseTally(key, display, category, count)

    companion object {
        private val KEY = Regex("[a-z0-9_]+")
        private const val PER_HUNDRED = 100.0
        private const val SMOOTHING = 0.5
        val CATEGORIES = setOf("reaction", "agree", "filler", "slang", "emotion", "polite", "laugh_word", "question", "meme")
    }
}
