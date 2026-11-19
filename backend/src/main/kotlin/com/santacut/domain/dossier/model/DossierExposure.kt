package com.santacut.domain.dossier.model

/**
 * 본인 메시지 수에 따른 장면 노출 구간(기획 2장 '노출 조건'). 임계값 100·300은 잠정이다.
 *
 * @property ledgerSize 입버릇 장부 줄 수. 0이면 장부 장을 뺀다.
 * @property extremeCount 극값 장 수
 * @property showsCaseFile 산타 조서 장을 넣는지
 * @property showsActs 「올해의 너 3막」 장을 넣는지
 * @property showsEmptyActs 화제가 없는 막도 빈 줄로 보여 줄지. 거짓이면 화제가 있는 막만 보인다.
 */
enum class DossierExposure(
    val ledgerSize: Int,
    val extremeCount: Int,
    val showsCaseFile: Boolean,
    val showsActs: Boolean,
    val showsEmptyActs: Boolean,
) {
    SPARSE(0, 2, false, false, false),
    MODERATE(3, 2, true, true, false),
    FULL(5, 3, true, true, true),
    ;

    fun isSparse(): Boolean = this == SPARSE

    companion object {
        const val MODERATE_FROM = 100
        const val FULL_FROM = 300

        fun of(messageCount: Int): DossierExposure = when {
            messageCount < MODERATE_FROM -> SPARSE
            messageCount < FULL_FROM -> MODERATE
            else -> FULL
        }
    }
}
