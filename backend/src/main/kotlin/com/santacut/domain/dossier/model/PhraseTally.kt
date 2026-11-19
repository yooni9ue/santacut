package com.santacut.domain.dossier.model

/**
 * 저장하는 입버릇 횟수 하나. 허용 어휘의 key, 표기, 분류만 담고 원문은 담지 않는다.
 */
data class PhraseTally(
    val key: String,
    val display: String,
    val category: String,
    val count: Int,
) {
    /** 입버릇 템플릿 슬롯(phrase, count). */
    fun slots(): Slots = Slots.of(PHRASE to display, COUNT to DossierFormat.times(count))

    companion object {
        private const val PHRASE = "phrase"
        private const val COUNT = "count"
    }
}
