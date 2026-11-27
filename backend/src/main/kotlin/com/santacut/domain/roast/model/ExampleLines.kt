package com.santacut.domain.roast.model

/**
 * 작가가 그대로 베끼면 안 되는 예시 문장 목록. 프롬프트 예시([RoastRules.promptExamples]), 유행어 예문, 서버 대체 문장(구간 문장, 통계 카드 문구, 펀치 틀 대체 문장)이다.
 * 띄어쓰기와 마침표·쉼표만 다른 문장도 같은 문장으로 본다. 사람마다 다르게 써야 할 문장이 예시 한 줄로 몰리지 않게 한다.
 */
class ExampleLines(texts: Collection<String>) {
    private val values: Set<String> = texts.map(::normalized).filter(String::isNotEmpty).toSet()

    /** [text]가 예시 문장과 같은지. */
    operator fun contains(text: String): Boolean = normalized(text) in values

    companion object {
        val EMPTY = ExampleLines(emptyList())
        private val IGNORED = Regex("[\\s.,]")

        private fun normalized(text: String): String = IGNORED.replace(text, "")
    }
}
