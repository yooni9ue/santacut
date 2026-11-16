package com.santacut.domain.analysis.model

/**
 * 본인 말에서 낱말 횟수로 주는 재미용 배지 규칙. 배지는 점수에 영향을 주지 않는다.
 * 순서가 보여 줄 우선순위다(공유 카드에는 첫 배지 하나만 싣는다).
 *
 * @property keywords 셀 낱말. 본인 줄에서만 센다.
 * @property minCount 이 횟수 이상이어야 받는다
 * @property maxCount 이 횟수 이하여야 받는다
 * @property title 배지 이름
 * @property countWord 라벨 앞에 붙일 대표 낱말
 */
enum class BadgeRule(
    val keywords: List<String>,
    private val minCount: Int,
    private val maxCount: Int,
    private val title: String,
    private val countWord: String,
) {
    THANKS_MACHINE(listOf("고마워", "고맙", "감사"), 5, Int.MAX_VALUE, "감사 자판기", "고마워"),
    CHEER_SQUAD(listOf("화이팅", "파이팅", "힘내"), 3, Int.MAX_VALUE, "응원 단장", "화이팅"),
    APOLOGY_ARTISAN(listOf("미안", "죄송"), 3, Int.MAX_VALUE, "사과 장인", "미안"),
    APOLOGY_SOLD_OUT(listOf("미안", "죄송"), 0, 0, "사과 품절", "미안"),
    LAUGH_FACTORY(listOf("ㅋㅋ", "ㅎㅎ"), 20, Int.MAX_VALUE, "웃음 제조기", "ㅋㅋ"),
    ;

    fun isEarnedBy(count: Int): Boolean = count in minCount..maxCount

    /** `고마워 12회 · 감사 자판기` 같은 표시 라벨. */
    fun label(count: Int): String = "$countWord ${count}회 · $title"
}
